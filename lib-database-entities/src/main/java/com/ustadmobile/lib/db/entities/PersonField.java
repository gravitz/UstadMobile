package com.ustadmobile.lib.db.entities;

import com.ustadmobile.lib.database.annotation.UmEntity;
import com.ustadmobile.lib.database.annotation.UmPrimaryKey;

/**
 * This Entity represents every field associated with the Person. This includes Core fields to be
 * displayed in the Edit/New/Detail pages.
 *
 * Note that fields are not associated with any specific
 * Person but apply to all Persons. Their values are mapped with the entity; PersonCustomFieldValue.
 *
 * The idea here is to build - for every core & custom
 * field - relevant label, icon and internal field name here.
 *
 * Any additional custom fields are to be added here. For eg: if you want to add a custom field
 * for measuring height of the person - you would add the relevant icon as String and field as
 * MessageID that maps to translation strings (which would be gotten via impl.getString(..) ). The
 * fieldName is internal and could just be "height of the person".
 *
 */
@UmEntity
public class PersonField {

    @UmPrimaryKey(autoIncrement = true)
    private long personCustomFieldUid;

    //Any extra field names that isn't used in the views.
    private String fieldName;

    //The label of the field used in the views.
    private int labelMessageId;

    //The field icon used in the view.
    private String fieldIcon;

    public long getPersonCustomFieldUid() {
        return personCustomFieldUid;
    }

    public void setPersonCustomFieldUid(long personCustomFieldUid) {
        this.personCustomFieldUid = personCustomFieldUid;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public int getLabelMessageId() {
        return labelMessageId;
    }

    public void setLabelMessageId(int labelMessageId) {
        this.labelMessageId = labelMessageId;
    }

    public String getFieldIcon() {
        return fieldIcon;
    }

    public void setFieldIcon(String fieldIcon) {
        this.fieldIcon = fieldIcon;
    }
}
