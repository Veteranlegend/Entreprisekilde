const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({ maxInstances: 10 });

const db = admin.firestore();

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

async function cleanupUserTokens(userId, validTokensToKeep = []) {
  if (!userId) return;

  const cleanedTokens = [...new Set(validTokensToKeep.filter(Boolean))];

  await db.collection("users").doc(userId).set(
    {
      fcmToken: cleanedTokens[0] || "",
      fcmTokens: cleanedTokens,
      lastTokenUpdatedAt: Date.now(),
    },
    { merge: true }
  );
}

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
      data: Object.fromEntries(
        Object.entries({
          title,
          body,
          ...data,
        }).map(([key, value]) => [key, String(value)])
      ),
      notification: {
        title,
        body,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "entreprisekilde_urgent_v2",
        },
      },
    });

    logger.info(`Notification send attempted to ${userId}`, {
      tokensCount: tokens.length,
      successCount: response.successCount,
      failureCount: response.failureCount,
    });

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

exports.sendMessageNotification = onDocumentCreated(
  "messageThreads/{threadId}/messages/{messageId}",
  async (event) => {
    const messageSnap = event.data;
    if (!messageSnap) return;

    const message = messageSnap.data();
    const threadId = event.params.threadId;

    const threadSnap = await db.collection("messageThreads").doc(threadId).get();
    if (!threadSnap.exists) {
      logger.warn(`Thread ${threadId} not found`);
      return;
    }

    const thread = threadSnap.data() || {};
    const participantIds = Array.isArray(thread.participantIds) ? thread.participantIds : [];
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

    let recipientId = explicitRecipientId;
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

    const senderName =
      typeof participantNames[senderId] === "string" && participantNames[senderId].trim() !== ""
        ? participantNames[senderId].trim()
        : "New message";

    const body = messageType === "image"
      ? "📷 Sent you an image"
      : (text || "You received a new message");

    await createInAppNotification({
      userId: recipientId,
      title: "New message",
      message: `${senderName}: ${body}`,
      type: "MESSAGE",
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