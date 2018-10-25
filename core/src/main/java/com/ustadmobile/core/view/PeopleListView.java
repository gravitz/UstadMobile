package com.ustadmobile.core.view;

import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.lib.db.entities.Person;
import com.ustadmobile.lib.db.entities.PersonWithEnrollment;

/**
 * PeopleList Core View extends Core UstadView. Will be implemented
 * on various implementations.
 */
public interface PeopleListView extends UstadView {

    String VIEW_NAME = "PeopleList";

    void setPeopleListProvider(UmProvider<PersonWithEnrollment> listProvider);


}
