package com.swparks.screenshots

import android.content.Context
import com.swparks.data.model.Comment
import com.swparks.data.model.Country
import com.swparks.data.model.Event
import com.swparks.data.model.Park
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.util.WorkoutAppJson
import com.swparks.util.readJSONFromAssets
import kotlinx.serialization.decodeFromString

object DemoData {
    const val screenshotLogin = "testuserapple"
    const val screenshotPassword = "111111"
    const val screenshotSearchQuery = "Ninenineone"

    private const val PARKS_ASSET_PATH = "parks.json"
    private const val COUNTRIES_ASSET_PATH = "countries.json"

    private const val PREVIEW_PARK_ID = 3L

    private const val SUNDAY_WORKOUT_DESCRIPTION = """
        <i data-ed3="3"></i><p>ВРЕМЯ ТРЕНИРОВКИ - 10:00</p><p>МЕСТО - ПАРК ПОБЕДЫ</p><p><br></p><p>Приложен маршрут от метро Минская.</p><p><br></p><p>Если что, у нас есть беседа в ТГ, чтобы можно было что-то оперативно спросить/уточнить/договориться - <a href="https://t.me/swmos" target="_blank">t.me/swmos</a></p><p><br></p><p>---</p><p><br></p><p>Воркаутеры и участники программы SOTKA в очередной раз соберутся для тренировки, общения и обмена опытом. Если у кого есть желание присоединиться, чтобы потренироваться вместе с нами в дружеской атмосфере старого доброго уличного воркаута, будем рады всем)</p><p><br></p><p>ДА, ПРИЙТИ МОЖНО, ДАЖЕ С ДЕТЬМИ, ДРУЗЬЯМИ, СВОИМ МОЛОДЫМ ЧЕЛОВЕКОМ И РОДИТЕЛЯМИ. МЫ БУДЕМ РАДЫ ВСЕМ!</p><p>ДА, ДАЖЕ, ЕСЛИ ВЫ НИКОГО НЕ ЗНАЕТЕ И НИЧЕГО НЕ УМЕЕТЕ (потому что мы сами друг с другом знакомимся и ничего феерического не умеем)!</p><p>ДА, ДАЖЕ, ЕСЛИ УРОВЕНЬ НА НУЛЕ!</p><p>ДА, ДАЖЕ, ЕСЛИ ВЫ ИЗ ДРУГОГО ГОРОДА, СЕЛА, ДЕРЕВНИ, ПЛАНЕТЫ!</p><p>ДА, МОЖНО ДАЖЕ ОПОЗДАТЬ (ничего страшного, потому что мы находимся там порядка двух часов)!</p><p>ДА, ЭТО БЕСПЛАТНО (потому что мы ничего не делаем)!</p><p><br></p><p>Главное - это ваше желание тренироваться, остальному научим, расскажем и покажем) мы даже колонку для музончика притащим, так что будет очень приятно, интересно и полезно.</p>
    """

    val demoUser = User(
        id = 24798L,
        name = "Workouter",
        image = "https://workout.su/uploads/avatars/2019/10/2019-10-07-01-10-08-yow.jpg",
        cityID = 1,
        countryID = 17,
        birthDate = "1989-11-25",
        email = "test@mail.ru",
        fullName = "",
        genderCode = 0,
        friendRequestCount = "0",
        friendsCount = 5,
        parksCount = "4",
        addedParks = null,
        journalCount = 2
    )

    val demoAuthorizedUser = User(
        id = 10367L,
        name = "testuserapple",
        image = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
        cityID = 1,
        countryID = 17,
        birthDate = "1992-08-12",
        email = "testuser@example.com",
        fullName = "Test User",
        genderCode = 0,
        friendRequestCount = "0",
        friendsCount = 0,
        parksCount = "0",
        addedParks = null,
        journalCount = 0
    )

    val demoSearchUser = User(
        id = 24798L,
        name = "Workouter",
        image = "https://workout.su/uploads/avatars/2019/10/2019-10-07-01-10-08-yow.jpg",
        cityID = 1,
        countryID = 17,
        birthDate = "1989-11-25",
        email = "test@mail.ru",
        fullName = "",
        genderCode = 0,
        friendsCount = 5,
        parksCount = "4",
        journalCount = 2,
        addedParks = null
    )

    private val previewComment = Comment(
        id = 2569L,
        body = "+ В центре парка, чистый воздух\r\n+ Кольца\r\n+ Тренажёры\r\n\r\n- Относительно далеко от метро",
        date = "2011-03-07T15:55:15+00:00",
        user = demoUser
    )

    val demoParkDetailPreview = Park(
        id = PREVIEW_PARK_ID,
        name = "№3 Средняя Легендарная",
        sizeID = 2,
        typeID = 6,
        longitude = "37.762597",
        latitude = "55.795396",
        address = "м. Партизанская, улица 2-я Советская",
        cityID = 1,
        countryID = 17,
        commentsCount = 1,
        preview = "https://workout.su/uploads/userfiles/измайлово.jpg",
        trainingUsersCount = null,
        createDate = "2011-03-07T22:55:15+03:00",
        author = null,
        photos = listOf(
            Photo(id = 1, photo = "https://workout.su/uploads/userfiles/измайлово.jpg"),
            Photo(id = 2, photo = "https://workout.su/uploads/userfiles/измайлово (2).jpg"),
            Photo(id = 3, photo = "https://workout.su/uploads/userfiles/измайлово-1.jpg"),
            Photo(id = 4, photo = "https://workout.su/uploads/userfiles/измайлово-3.jpg"),
            Photo(id = 5, photo = "https://workout.su/uploads/userfiles/измайлово-2.jpg"),
            Photo(id = 6, photo = "https://workout.su/uploads/userfiles/измайлово-4.jpg")
        ),
        comments = listOf(previewComment),
        trainHere = false,
        equipmentIDS = null,
        mine = null,
        canEdit = null,
        trainingUsers = listOf(demoUser, demoUser)
    )

    private val vasilenAuthor = User(
        id = 4646L,
        name = "VASILEN",
        image = "https://workout.su/uploads/avatars/2018/06/2018-06-20-15-06-12-whv.png"
    )

    val demoPastEvents: List<Event> = listOf(
        sundayWorkoutEvent(
            id = 4699L,
            title = "Тестирование максимумов | Открытая Воскресная Тренировка #48 в 2025 году (участники SOTKA, воркаутеры, все желающие)",
            beginDate = "2025-11-23T07:00:00+00:00",
            previewImage = "https://workout.su/thumbs/6_100x100_FFFFFF//uploads/userfiles/2025/11/2025-11-18-22-11-33-waf.jpg",
            photo1 = "https://workout.su/uploads/userfiles/2025/11/2025-11-18-22-11-33-waf.jpg",
            photo2 = "https://workout.su/uploads/userfiles/2025/11/2025-11-18-22-11-41-k0p.png",
            photo3 = "https://workout.su/uploads/userfiles/2025/11/2025-11-18-22-11-41-e12.png"
        ),
        sundayWorkoutEvent(
            id = 4698L,
            title = "Открытая Воскресная Тренировка #47 в 2025 году (участники SOTKA, воркаутеры, все желающие)",
            beginDate = "2025-11-16T07:00:00+00:00",
            previewImage = "https://workout.su/thumbs/6_100x100_FFFFFF//uploads/userfiles/2025/11/2025-11-10-21-11-44-l_u.jpg",
            photo1 = "https://workout.su/uploads/userfiles/2025/11/2025-11-10-21-11-44-l_u.jpg",
            photo2 = "https://workout.su/uploads/userfiles/2025/11/2025-11-10-21-11-13-pq1.png",
            photo3 = "https://workout.su/uploads/userfiles/2025/11/2025-11-10-21-11-38-smk.png"
        ),
        sundayWorkoutEvent(
            id = 4695L,
            title = "Открытая Воскресная Тренировка #46 в 2025 году (участники SOTKA, воркаутеры, все желающие)",
            beginDate = "2025-11-09T07:00:00+00:00",
            previewImage = "https://workout.su/thumbs/6_100x100_FFFFFF//uploads/userfiles/2025/11/2025-11-05-21-11-21-4nq.jpg",
            photo1 = "https://workout.su/uploads/userfiles/2025/11/2025-11-05-21-11-21-4nq.jpg",
            photo2 = "https://workout.su/uploads/userfiles/2025/11/2025-11-05-21-11-59-grr.png",
            photo3 = "https://workout.su/uploads/userfiles/2025/11/2025-11-05-21-11-59-grn.png"
        ),
        sundayWorkoutEvent(
            id = 4694L,
            title = "Открытая Воскресная Тренировка #45 в 2025 году (участники SOTKA, воркаутеры, все желающие)",
            beginDate = "2025-11-02T07:00:00+00:00",
            previewImage = "https://workout.su/thumbs/6_100x100_FFFFFF//uploads/userfiles/2025/10/2025-10-27-20-10-45-mq9.jpg",
            photo1 = "https://workout.su/uploads/userfiles/2025/10/2025-10-27-20-10-45-mq9.jpg",
            photo2 = "https://workout.su/uploads/userfiles/2025/10/2025-10-27-20-10-53-wu6.png",
            photo3 = "https://workout.su/uploads/userfiles/2025/10/2025-10-27-20-10-53-aig.png"
        ),
        sundayWorkoutEvent(
            id = 4693L,
            title = "Открытая Воскресная Тренировка #44 в 2025 году (участники SOTKA, воркаутеры, все желающие)",
            beginDate = "2025-10-26T07:00:00+00:00",
            previewImage = "https://workout.su/thumbs/6_100x100_FFFFFF//uploads/userfiles/2025/10/2025-10-20-21-10-28--a2.jpg",
            photo1 = "https://workout.su/uploads/userfiles/2025/10/2025-10-20-21-10-28--a2.jpg",
            photo2 = "https://workout.su/uploads/userfiles/2025/10/2025-10-20-21-10-42-hoa.png",
            photo3 = "https://workout.su/uploads/userfiles/2025/10/2025-10-20-21-10-42-piy.png"
        )
    )

    val demoFutureEvents: List<Event> = emptyList()

    fun loadDemoParks(context: Context): List<Park> =
        parseAssetJson(context = context, path = PARKS_ASSET_PATH)

    fun loadDemoCountries(context: Context): List<Country> =
        parseAssetJson(context = context, path = COUNTRIES_ASSET_PATH)

    fun demoParksForUser(parks: List<Park>): List<Park> = parks.take(4)

    fun parkDetailsById(parkId: Long, fallback: Park?): Park {
        if (parkId == PREVIEW_PARK_ID) return demoParkDetailPreview
        return fallback ?: demoParkDetailPreview
    }

    fun eventById(eventId: Long): Event =
        demoPastEvents.firstOrNull { it.id == eventId } ?: demoPastEvents.first()

    fun searchUsers(query: String): List<User> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()
        val matchesName = demoSearchUser.name.contains(normalizedQuery, ignoreCase = true)
        val matchesFullName = demoSearchUser.fullName?.contains(normalizedQuery, ignoreCase = true) == true
        val matchesScreenshotScenario = normalizedQuery.equals(screenshotSearchQuery, ignoreCase = true)
        return if (matchesName || matchesFullName || matchesScreenshotScenario) {
            listOf(demoSearchUser)
        } else {
            emptyList()
        }
    }

    private fun sundayWorkoutEvent(
        id: Long,
        title: String,
        beginDate: String,
        previewImage: String,
        photo1: String,
        photo2: String,
        photo3: String
    ): Event = Event(
        id = id,
        title = title,
        description = SUNDAY_WORKOUT_DESCRIPTION,
        beginDate = beginDate,
        countryID = 17,
        cityID = 1,
        commentsCount = 0,
        preview = previewImage,
        parkID = 5464L,
        latitude = "55.72681766162947",
        longitude = "37.50063106774381",
        trainingUsersCount = 1,
        isCurrent = false,
        address = null,
        photos = listOf(
            Photo(id = 1, photo = photo1),
            Photo(id = 2, photo = photo2),
            Photo(id = 3, photo = photo3)
        ),
        trainingUsers = listOf(vasilenAuthor),
        author = vasilenAuthor,
        name = title,
        comments = emptyList(),
        isOrganizer = false,
        canEdit = false,
        trainHere = false
    )

    private inline fun <reified T> parseAssetJson(context: Context, path: String): T {
        val jsonString = readJSONFromAssets(context, path)
        check(jsonString.isNotBlank()) {
            "Не удалось прочитать demo-data из assets/$path"
        }
        return WorkoutAppJson.decodeFromString(jsonString)
    }
}
