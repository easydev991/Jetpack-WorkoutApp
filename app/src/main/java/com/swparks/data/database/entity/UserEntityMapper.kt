package com.swparks.data.database.entity

import com.swparks.data.model.User

/**
 * Конвертировать [User] в [UserEntity] с указанием флагов категоризации
 *
 * @param isCurrentUser Является ли текущим авторизованным пользователем
 * @param isFriend Является ли другом
 * @param isFriendRequest Есть ли заявка в друзья
 * @param isBlacklisted Добавлен ли в черный список
 * @return [UserEntity] с соответствующими флагами и добавленными площадками
 */
fun User.toEntity(
    isCurrentUser: Boolean = false,
    isFriend: Boolean = false,
    isFriendRequest: Boolean = false,
    isBlacklisted: Boolean = false
): UserEntity =
    UserEntity(
        id = id,
        name = name,
        image = image,
        cityId = cityID,
        countryId = countryID,
        birthDate = birthDate,
        email = email,
        fullName = fullName,
        genderCode = genderCode,
        friendRequestCount = friendRequestCount,
        friendsCount = friendsCount,
        parksCount = parksCount,
        addedParks = addedParks,
        journalCount = journalCount,
        isCurrentUser = isCurrentUser,
        isFriend = isFriend,
        isFriendRequest = isFriendRequest,
        isBlacklisted = isBlacklisted
    )

/**
 * Конвертировать [UserEntity] в [User]
 *
 * @return [User] без флагов категоризации и с восстановленными добавленными площадками
 */
fun UserEntity.toDomain(): User =
    User(
        id = id,
        name = name,
        image = image,
        cityID = cityId,
        countryID = countryId,
        birthDate = birthDate,
        email = email,
        fullName = fullName,
        genderCode = genderCode,
        friendRequestCount = friendRequestCount,
        friendsCount = friendsCount,
        parksCount = parksCount,
        addedParks = addedParks,
        journalCount = journalCount
    )
