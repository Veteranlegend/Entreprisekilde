const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({ maxInstances: 10 });

const db = admin.firestore();

function getValidTokens(userDoc) {
  const singleToken = userDoc.get("fcmToken");
  const tokenArray = userDoc.get("fcmTokens");

  const tokens = [];

  if (typeof singleToken === "string" && singleToken.trim() !== "") {
    tokens.push(singleToken.trim());
  }

  if (Array.isArray(tokenArray)) {
    for (const token of tokenArray) {
      if (typeof token === "string" && token.trim() !== "") {
        tokens.push(token.trim());
      }
    }
  }

  return [...new Set(tokens)];
}

async function removeInvalidTokens(userId, invalidTokens) {
  if (!userId || !Array.isArray(invalidTokens) || invalidTokens.length === 0) return;

  const userRef = db.collection("users").doc(userId);
  const userDoc = await userRef.get();

  if (!userDoc.exists) return;

  const currentSingleToken = userDoc.get("fcmToken");
  const currentTokenArray = userDoc.get("fcmTokens");

  const cleanedArray = Array.isArray(currentTokenArray)
    ? currentTokenArray.filter(
        (token) =>
          typeof token === "string" &&
          token.trim() !== "" &&
          !invalidTokens.includes(token.trim())
      )
    : [];

  const updates = {
    fcmTokens: cleanedArray,
  };

  if (
    typeof currentSingleToken === "string" &&
    currentSingleToken.trim() !== "" &&
    invalidTokens.includes(currentSingleToken.trim())
  ) {
    updates.fcmToken = cleanedArray.length > 0 ? cleanedArray[0] : admin.firestore.FieldValue.delete();
  }

  await userRef.set(updates, { merge: true });
}

async function sendNotificationToUser(userId, title, body, data = {}) {
  if (!userId) return;

  const userDoc = await db.collection("users").doc(userId).get();
  if (!userDoc.exists) {
    logger.warn(`User ${userId} not found`);
    return;
  }

  const tokens = getValidTokens(userDoc);

  if (tokens.length === 0) {
    logger.warn(`User ${userId} has no valid FCM tokens`);
    return;
  }

  try {
    const response = await admin.messaging().sendEachForMulticast({
      tokens,
      notification: {
        title,
        body,
      },
      data: Object.fromEntries(
        Object.entries(data).map(([key, value]) => [key, String(value)])
      ),
      android: {
        priority: "high",
        notification: {
          channelId: "entreprisekilde_urgent_v2",
          sound: "default",
        },
      },
    });

    logger.info(`Notification send result for ${userId}`, {
      successCount: response.successCount,
      failureCount: response.failureCount,
      tokensCount: tokens.length,
    });

    const invalidTokens = [];

    response.responses.forEach((result, index) => {
      if (!result.success) {
        const errorCode = result.error?.code || "";
        logger.error(`Failed token for user ${userId}`, {
          token: tokens[index],
          code: errorCode,
          message: result.error?.message || "Unknown error",
        });

        if (
          errorCode === "messaging/invalid-registration-token" ||
          errorCode === "messaging/registration-token-not-registered"
        ) {
          invalidTokens.push(tokens[index]);
        }
      }
    });

    if (invalidTokens.length > 0) {
      await removeInvalidTokens(userId, invalidTokens);
      logger.warn(`Removed invalid tokens for user ${userId}`, { invalidTokens });
    }
  } catch (error) {
    logger.error(`Failed to send notification to ${userId}`, error);
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
    const text = typeof message.text === "string" ? message.text.trim() : "";

    if (!senderId) {
      logger.warn(`Message ${event.params.messageId} in thread ${threadId} has no senderId`);
      return;
    }

    const recipientId = participantIds.find(
      (id) => typeof id === "string" && id.trim() !== "" && id.trim() !== senderId
    );

    if (!recipientId) {
      logger.warn(`No recipient found for thread ${threadId}`);
      return;
    }

    const senderName =
      typeof participantNames[senderId] === "string" && participantNames[senderId].trim() !== ""
        ? participantNames[senderId].trim()
        : "New message";

    await sendNotificationToUser(
      recipientId,
      senderName,
      text || "You received a new message",
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

    await sendNotificationToUser(
      assignedUserId,
      "New task assigned",
      `${customer}: ${taskDetails}`,
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

    await sendNotificationToUser(
      afterAssigned,
      "Task assigned to you",
      `${customer}: ${taskDetails}`,
      {
        type: "task",
        taskId,
        senderUserId: "system",
        recipientUserId: afterAssigned,
      }
    );
  }
);