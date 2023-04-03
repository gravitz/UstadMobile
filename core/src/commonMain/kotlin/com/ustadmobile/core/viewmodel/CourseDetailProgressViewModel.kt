package com.ustadmobile.core.viewmodel

import com.ustadmobile.lib.db.entities.Person

data class CourseDetailProgressUiState(

    val students: List<Person> = emptyList(),

    val results: List<String> = emptyList(),

    val fieldsEnabled: Boolean = true,

)