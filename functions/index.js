const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({ maxInstances: 10 });

const db = admin.firestore();

async function sendNotificationToUser(userId, title, body, data = {}) {
  if (!userId) return;

  const userDoc = await db.collection("users").doc(userId).get();
  if (!userDoc.exists) {
    logger.warn(`User ${userId} not found`);
    return;
  }

  const token = userDoc.get("fcmToken");
  if (!token || typeof token !== "string" || token.trim() === "") {
    logger.warn(`User ${userId} has no fcmToken`);
    return;
  }

  try {
    const response = await admin.messaging().send({
      token,
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

    logger.info(`Notification sent to ${userId}`, { response });
  } catch (error) {
    logger.error(`Failed to send notification to ${userId}`, error);

    const code = error?.code || "";
    if (
      code === "messaging/invalid-registration-token" ||
      code === "messaging/registration-token-not-registered"
    ) {
      await db.collection("users").doc(userId).set(
        { fcmToken: admin.firestore.FieldValue.delete() },
        { merge: true }
      );
      logger.warn(`Removed invalid token for user ${userId}`);
    }
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

    const senderId = typeof message.senderId === "string" ? message.senderId : "";
    const text = typeof message.text === "string" ? message.text.trim() : "";

    const recipientId = participantIds.find((id) => id !== senderId);
    if (!recipientId) {
      logger.warn(`No recipient found for thread ${threadId}`);
      return;
    }

    const senderName =
      typeof participantNames[senderId] === "string" && participantNames[senderId].trim() !== ""
        ? participantNames[senderId]
        : "New message";

    await sendNotificationToUser(
      recipientId,
      senderName,
      text || "You received a new message",
      {
        type: "chat",
        threadId,
        senderId,
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
        assignedUserId,
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
        assignedUserId: afterAssigned,
      }
    );
  }
);