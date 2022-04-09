package com.ustadmobile.core.view

import com.ustadmobile.door.DoorDataSourceFactory
import com.ustadmobile.lib.db.entities.Chat
import com.ustadmobile.lib.db.entities.MessageWithPerson


interface ChatDetailView: UstadDetailView<Chat> {

    var title : String?

    var messageList: DoorDataSourceFactory<Int, MessageWithPerson>?

    companion object {

        const val VIEW_NAME = "ChatDetailView"

    }

}