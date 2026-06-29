package com.waleed.crm.data.room

import com.waleed.crm.data.*

fun Client.toEntity() = ClientEntity(id, name.stripDoctorPrefix(), phone, secondPhone, clientType, specialization, clientClass, location, isClassified, cardColor, dateAdded, updatedAt, notes)
fun ClientEntity.toModel() = Client(id, name.doctorDisplayName(clientType), phone.withYemenPhoneCode(), secondPhone, clientType, specialization, clientClass, location, isClassified, cardColor, dateAdded, updatedAt, notes)
fun MessageLog.toEntity() = MessageLogEntity(id, clientId, timestamp, messageText, attachmentName, attachmentType, sendMode, campaignId, status)
fun MessageLogEntity.toModel() = MessageLog(id, clientId, timestamp, messageText, attachmentName, attachmentType, sendMode, campaignId, status)
fun FollowUp.toEntity() = FollowUpEntity(id, clientId, title, dueAt, status, notes, createdAt)
fun FollowUpEntity.toModel() = FollowUp(id, clientId, title, dueAt, status, notes, createdAt)
fun UserAccount.toEntity() = UserEntity(id, name, username, passwordHash, role, isActive, createdAt, lastLogin)
fun UserEntity.toModel() = UserAccount(id, name, username, passwordHash, role, isActive, createdAt, lastLogin)
fun AuditLog.toEntity() = AuditLogEntity(id, username, action, entityType, entityId, entityName, details, createdAt)
fun AuditLogEntity.toModel() = AuditLog(id, username, action, entityType, entityId, entityName, details, createdAt)
fun SavedSegment.toEntity() = SavedSegmentEntity(id, name, query, clientType, specialization, location, clientClass, onlyPendingFollowUp, onlyOverdueFollowUp, createdAt)
fun SavedSegmentEntity.toModel() = SavedSegment(id, name, query, clientType, specialization, location, clientClass, onlyPendingFollowUp, onlyOverdueFollowUp, createdAt)
