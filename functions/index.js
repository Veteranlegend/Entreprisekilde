const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

// Initialize the Firebase Admin SDK once so Firestore and FCM can be used server-side.
admin.initializeApp();

// Limit how many function instances can run at the same time.
// This helps avoid uncontrolled scaling for this small notification workload.
setGlobalOptions({ maxInstances: 10 });

const db = admin.firestore();

/**
 * Collects all valid FCM tokens stored on a user document.
 *
 * We support both:
 * - a legacy single-token field: fcmToken
 * - a newer multi-token field: fcmTokens
 *
 * The final result is deduplicated so we do not send the same push more than once
 * to the same physical token.
 */
function getAllValidTokens(userDoc) {
  const tokens = [];

  const singleToken = userDoc.get("fcmToken");
  if (typeof singleToken === "string" && singleToken.trim() !== "") {
    tokens.push(singleToken.trim());
  }

  const tokenArray = userDoc.get("fcmTokens");
  if (Array.isArray(tokenArray)) {
    for (const token of tokenArray) {
      if (typeof token === "string" && token.trim() !== "") {
        tokens.push(token.trim());
      }
    }
  }

  return [...new Set(tokens)];
}

/**
 * Rewrites the user's stored token fields with only the tokens we still consider valid.
 *
 * This is useful after sending a multicast push, because Firebase can tell us which
 * tokens worked and which ones failed. That lets us automatically clean up stale tokens.
 */
async function cleanupUserTokens(userId, validTokensToKeep = []) {
  if (!userId) return;

  const cleanedTokens = [...new Set(validTokensToKeep.filter(Boolean))];

  await db.collection("users").doc(userId).set(
    {
      // Keep the first valid token in the legacy single-token field for backward compatibility.
      fcmToken: cleanedTokens[0] || "",

      // Store the full cleaned token list in the newer array-based field.
      fcmTokens: cleanedTokens,

      // Useful for debugging and tracking when token state was last refreshed.
      lastTokenUpdatedAt: Date.now(),
    },
    { merge: true }
  );
}

/**
 * Creates an in-app notification record in Firestore.
 *
 * This powers the notifications screen inside the app, independent of whether
 * the push notification was successfully delivered to the device.
 */
async function createInAppNotification({
  userId,
  title,
  message,
  type,
  relatedThreadId = null,
}) {
  if (!userId) return;

  await db.collection("notifications").add({
    userId,
    title,
    message,
    type,
    createdAt: Date.now(),
    isRead: false,
    relatedThreadId,
  });
}

/**
 * Sends a push notification to all valid tokens for a given user.
 *
 * The function:
 * - loads the user's tokens
 * - sends one multicast request
 * - logs successes/failures
 * - removes tokens that failed
 *
 * We send both a "notification" payload and a "data" payload so the app can
 * both display the push and react to it programmatically when needed.
 */
async function sendNotificationToUser(userId, title, body, data = {}) {
  if (!userId) return;

  const userRef = db.collection("users").doc(userId);
  const userDoc = await userRef.get();

  if (!userDoc.exists) {
    logger.warn(`User ${userId} not found`);
    return;
  }

  const tokens = getAllValidTokens(userDoc);

  if (!tokens.length) {
    logger.warn(`User ${userId} has no valid FCM tokens`);
    return;
  }

  try {
    const response = await admin.messaging().sendEachForMulticast({
      tokens,

      // FCM data values must be strings, so we normalize everything before sending.
      data: Object.fromEntries(
        Object.entries({
          title,
          body,
          ...data,
        }).map(([key, value]) => [key, String(value)])
      ),

      // Standard visible notification payload.
      notification: {
        title,
        body,
      },

      // Android-specific settings.
      android: {
        priority: "high",
        notification: {
          // This should match a notification channel created in the Android app.
          channelId: "entreprisekilde_urgent_v2",
        },
      },
    });

    logger.info(`Notification send attempted to ${userId}`, {
      tokensCount: tokens.length,
      successCount: response.successCount,
      failureCount: response.failureCount,
    });

    // Keep only tokens that succeeded so we can clean out invalid/stale ones.
    const validTokens = [];
    response.responses.forEach((result, index) => {
      const token = tokens[index];

      if (result.success) {
        validTokens.push(token);
        return;
      }

      const code = result.error?.code || "";
      logger.error(`Failed for token`, {
        userId,
        token,
        code,
        message: result.error?.message || "Unknown error",
      });
    });

    await cleanupUserTokens(userId, validTokens);
  } catch (error) {
    logger.error(`Failed to send notification to ${userId}`, {
      userId,
      code: error?.code || "",
      message: error?.message || "Unknown error",
    });
  }
}

/**
 * Triggered whenever a new message is created inside a message thread.
 *
 * Flow:
 * 1. Read the new message
 * 2. Load the thread to figure out participants
 * 3. Determine who the recipient should be
 * 4. Create an in-app notification
 * 5. Send a push notification to the recipient
 */
exports.sendMessageNotification = onDocumentCreated(
  "messageThreads/{threadId}/messages/{messageId}",
  async (event) => {
    const messageSnap = event.data;
    if (!messageSnap) return;

    const message = messageSnap.data();
    const threadId = event.params.threadId;

    // Load the parent thread so we can resolve participants and display names.
    const threadSnap = await db.collection("messageThreads").doc(threadId).get();
    if (!threadSnap.exists) {
      logger.warn(`Thread ${threadId} not found`);
      return;
    }

    const thread = threadSnap.data() || {};

    // participantIds is expected to be an array of user ids in the conversation.
    const participantIds = Array.isArray(thread.participantIds) ? thread.participantIds : [];

    // participantNames is expected to be a map like { userId: "Display Name" }.
    const participantNames =
      thread.participantNames && typeof thread.participantNames === "object"
        ? thread.participantNames
        : {};

    const senderId = typeof message.senderId === "string" ? message.senderId.trim() : "";
    const explicitRecipientId =
      typeof message.recipientUserId === "string" ? message.recipientUserId.trim() : "";
    const text = typeof message.text === "string" ? message.text.trim() : "";
    const messageType =
      typeof message.messageType === "string" ? message.messageType.trim() : "text";

    if (!senderId) {
      logger.warn(`Message ${event.params.messageId} in thread ${threadId} has no senderId`);
      return;
    }

    // Prefer an explicitly stored recipient if present.
    let recipientId = explicitRecipientId;

    // If no explicit recipient was saved, try to infer it from the thread participants.
    // This works best for one-to-one chats.
    if (!recipientId) {
      const otherParticipants = participantIds.filter(
        (id) => typeof id === "string" && id.trim() !== "" && id.trim() !== senderId
      );

      if (otherParticipants.length === 1) {
        recipientId = otherParticipants[0].trim();
      }
    }

    if (!recipientId) {
      logger.warn(`Could not determine recipient for thread ${threadId}`);
      return;
    }

    // Use the sender's display name if available, otherwise fall back to a generic label.
    const senderName =
      typeof participantNames[senderId] === "string" && participantNames[senderId].trim() !== ""
        ? participantNames[senderId].trim()
        : "New message";

    // Give image messages a friendlier preview in both push and in-app notifications.
    const body = messageType === "image"
      ? "📷 Sent you an image"
      : (text || "You received a new message");

    await createInAppNotification({
      userId: recipientId,
      title: "New message",
      message: `${senderName}: ${body}`,
      type: "MESSAGE",

      // Stored as Number here because the app appears to expect a numeric related thread id.
      relatedThreadId: Number(threadId),
    });

    await sendNotificationToUser(
      recipientId,
      senderName,
      body,
      {
        type: "chat",
        threadId,
        senderUserId: senderId,
        recipientUserId: recipientId,
      }
    );
  }
);

/**
 * Triggered when a new task document is created.
 *
 * If the task has an assigned user, we create both:
 * - an in-app notification
 * - a push notification
 */
exports.sendTaskAssignedNotificationOnCreate = onDocumentCreated(
  "tasks/{taskId}",
  async (event) => {
    const taskSnap = event.data;
    if (!taskSnap) return;

    const task = taskSnap.data();
    const taskId = event.params.taskId;

    const assignedUserId =
      typeof task.assignedUserId === "string" ? task.assignedUserId.trim() : "";

    if (!assignedUserId) {
      logger.warn(`Task ${taskId} has no assignedUserId`);
      return;
    }

    // Build a user-friendly notification message using whatever task data is available.
    const customer =
      typeof task.customer === "string" && task.customer.trim() !== ""
        ? task.customer.trim()
        : "A new task";

    const taskDetails =
      typeof task.taskDetails === "string" && task.taskDetails.trim() !== ""
        ? task.taskDetails.trim()
        : "You have been assigned a new task";

    const message = `${customer}: ${taskDetails}`;

    await createInAppNotification({
      userId: assignedUserId,
      title: "Task assigned",
      message,
      type: "TASK_ASSIGNED",
      relatedThreadId: null,
    });

    await sendNotificationToUser(
      assignedUserId,
      "New task assigned",
      message,
      {
        type: "task",
        taskId,
        senderUserId: "system",
        recipientUserId: assignedUserId,
      }
    );
  }
);

/**
 * Triggered whenever a task document is updated.
 *
 * We only send a notification when the assigned user changes.
 * That prevents unnecessary pushes on unrelated task edits.
 */
exports.sendTaskAssignedNotificationOnUpdate = onDocumentUpdated(
  "tasks/{taskId}",
  async (event) => {
    const before = event.data?.before?.data() || {};
    const after = event.data?.after?.data() || {};
    const taskId = event.params.taskId;

    const beforeAssigned =
      typeof before.assignedUserId === "string" ? before.assignedUserId.trim() : "";
    const afterAssigned =
      typeof after.assignedUserId === "string" ? after.assignedUserId.trim() : "";

    // Skip when there is no assignee or the assignee did not actually change.
    if (!afterAssigned || beforeAssigned === afterAssigned) {
      return;
    }

    const customer =
      typeof after.customer === "string" && after.customer.trim() !== ""
        ? after.customer.trim()
        : "A task";

    const taskDetails =
      typeof after.taskDetails === "string" && after.taskDetails.trim() !== ""
        ? after.taskDetails.trim()
        : "You have been assigned a task";

    const message = `${customer}: ${taskDetails}`;

    await createInAppNotification({
      userId: afterAssigned,
      title: "Task assigned",
      message,
      type: "TASK_ASSIGNED",
      relatedThreadId: null,
    });

    await sendNotificationToUser(
      afterAssigned,
      "Task assigned to you",
      message,
      {
        type: "task",
        taskId,
        senderUserId: "system",
        recipientUserId: afterAssigned,
      }
    );
  }
);