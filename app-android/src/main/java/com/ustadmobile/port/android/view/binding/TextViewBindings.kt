package com.ustadmobile.port.android.view.binding

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import android.widget.EditText
import android.widget.SeekBar
import android.view.View
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.toughra.ustadmobile.R
import com.ustadmobile.core.contentformats.xapi.Statement
import com.ustadmobile.core.model.BitmaskFlag
import com.ustadmobile.core.model.BitmaskMessageId
import com.ustadmobile.core.util.MessageIdOption
import com.ustadmobile.core.util.UMFileUtil
import com.ustadmobile.core.util.ext.*
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.db.entities.VerbEntity.Companion.VERB_ANSWERED_UID
import java.util.*
import java.util.concurrent.TimeUnit
import com.soywiz.klock.DateFormat as KlockDateFormat

@BindingAdapter("textMessageId")
fun TextView.setTextMessageId(messageId: Int) {
    text = systemImpl.getString(messageId, context)
}

@BindingAdapter("hintMessageId")
fun TextView.setHintMessageId(messageId: Int) {
    hint = systemImpl.getString(messageId, context)
}

@BindingAdapter("customFieldHint")
fun TextView.setCustomFieldHint(customField: CustomField?) {
    hint = if (customField != null) {
        systemImpl.getString(customField.customFieldLabelMessageID, context)
    } else {
        ""
    }
}

@BindingAdapter(value = ["textBitmaskValue", "textBitmaskFlags"], requireAll = false)
fun TextView.setBitmaskListText(textBitmaskValue: Long?, textBitmaskFlags: List<BitmaskFlag>?) {
    if (textBitmaskValue == null || textBitmaskFlags == null)
        return

    text = textBitmaskFlags.filter { (it.flagVal and textBitmaskValue) == it.flagVal }
            .joinToString { systemImpl.getString(it.messageId, context) }
}

@BindingAdapter(value = ["bitmaskValue", "flagMessageIds"], requireAll = false)
fun TextView.setBitmaskListTextFromMap(bitmaskValue: Long?, flagMessageIds: List<BitmaskMessageId>?) {
    if(bitmaskValue == null || flagMessageIds == null)
        return

    val impl = systemImpl

    text = flagMessageIds.map { it.toBitmaskFlag(bitmaskValue) }
        .filter { it.enabled }
        .joinToString { impl.getString(it.messageId, context) }
}

/**
 * This binder will handle situations where a there is a fixed list of flags, each of which
 * corresponds to a given messageId.
 *
 * e.g.
 *
 * class MyPresenter {
 *    companion object {
 *       @JvmField
 *       val ROLE_MAP = mapOf(ClazzMember.ROLE_STUDENT to MessageID.student,
 *                       ClazzMember.ROLE_TEACHER to MessageID.teacher)
 *    }
 * }
 *
 * You can then use the following in the view XML:
 *
 * &lt;import class="com.packagepath.MyPresenter"/&gt;
 *
 * &lt;TextView
 * ...
 * app:textMessageIdLookupKey="@{entityObject.memberRole}"
 * app:textMessageIdLookupMap="@{MyPresenter.ROLE_MAP}"
 * /&gt;
 *
 * Note textMessageIdLookupKey and textMessageIdLookupMap are in separate binders because if they
 * are in the same binder the generated data binding does not always update it when one is set
 * after the other.
 */
@BindingAdapter("textMessageIdLookupKey")
fun TextView.setTextMessageIdOptionSelected(textMessageIdLookupKey: Int) {
    setTag(R.id.tag_messageidoption_selected, textMessageIdLookupKey)
    updateFromTextMessageIdOptions()
}

@BindingAdapter(value = ["textMessageIdLookupMap", "fallbackMessageId", "fallbackMessage"], requireAll = false)
fun TextView.setTextMessageIdOptions(textMessageIdLookupMap: Map<Int, Int>?,
                                     fallbackMessageId: Int?, fallbackMessage: String?) {
    setTag(R.id.tag_messageidoptions_list, textMessageIdLookupMap)
    setTag(R.id.tag_messageidoption_fallback, fallbackMessage ?:
        fallbackMessageId?.let { systemImpl.getString(it, context) } ?: "")

    updateFromTextMessageIdOptions()
}

@SuppressLint("SetTextI18n")
private fun TextView.updateFromTextMessageIdOptions() {
    val currentOption = getTag(R.id.tag_messageidoption_selected) as? Int
    val textMessageIdOptions = getTag(R.id.tag_messageidoptions_list) as? Map<Int, Int>
    val fallbackMessage = getTag(R.id.tag_messageidoption_fallback) as? String
    if(currentOption != null && textMessageIdOptions != null) {
        val messageId = textMessageIdOptions[currentOption]
        if(messageId != null) {
            text = systemImpl.getString(messageId, context)
        }else if(fallbackMessage != null) {
            text = fallbackMessage
        }
    }
}

@BindingAdapter(value= ["textMessageIdOptionSelected","textMessageIdOptions"], requireAll = true)
fun TextView.setTextFromMessageIdList(textMessageIdOptionSelected: Int, textMessageIdOptions: List<MessageIdOption>) {
    text = systemImpl.getString(textMessageIdOptions
            ?.firstOrNull { it.code == textMessageIdOptionSelected }?.messageId ?: 0, context)
}

@BindingAdapter(value = ["textCustomFieldValue", "textCustomFieldValueOptions"])
fun TextView.setTextFromCustomFieldDropDownOption(customFieldValue: CustomFieldValue?,
                                                  customFieldValueOptions: List<CustomFieldValueOption>?) {
    val selectedOption = customFieldValueOptions
            ?.firstOrNull { it.customFieldValueOptionUid == customFieldValue?.customFieldValueCustomFieldValueOptionUid }
    if (selectedOption != null) {
        text = if (selectedOption.customFieldValueOptionMessageId != 0) {
            systemImpl.getString(selectedOption.customFieldValueOptionMessageId, context)
        } else {
            selectedOption.customFieldValueOptionName ?: ""
        }
    } else {
        text = ""
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter(value = ["textFromDateLong", "textToDateLong"])
fun TextView.setTextFromToDateLong(textFromDateLong: Long, textToDateLong: Long) {
    val dateFormat = DateFormat.getDateFormat(context)
    text = "${if (textFromDateLong > 0) dateFormat.format(textFromDateLong) else ""} -" +
            " ${if (textToDateLong > 0 && textToDateLong != Long.MAX_VALUE) dateFormat.format(textToDateLong) else ""}"
}

@SuppressLint("SetTextI18n")
@BindingAdapter(value = ["enrolmentTextFromDateLong", "enrolmentTextToDateLong"])
fun TextView.setEnrolmentTextFromToDateLong(textFromDateLong: Long, textToDateLong: Long) {
    val dateFormat = DateFormat.getDateFormat(context)
    text = "${if (textFromDateLong > 0) dateFormat.format(textFromDateLong) else ""} -" +
            " ${if (textToDateLong > 0 && textToDateLong != Long.MAX_VALUE) dateFormat.format(textToDateLong) else context.getString(R.string.time_present)}"
}



private val textViewSchoolGenderStringIds: Map<Int, Int> = mapOf(
        School.SCHOOL_GENDER_MIXED to R.string.mixed,
        School.SCHOOL_GENDER_FEMALE to R.string.female,
        School.SCHOOL_GENDER_MALE to R.string.male
)


@BindingAdapter("textSchoolGender")
fun TextView.setSchoolGenderText(gender: Int) {
    val genderStringId = textViewSchoolGenderStringIds[gender]
    text = if (genderStringId != null) {
        context.getString(genderStringId)
    } else {
        ""
    }
}

@BindingAdapter(value = ["inventoryDescriptionStockCount", "inventoryDescriptionWeNames"])
fun TextView.setInventoryDescription(stockCount: Int, weNames: String){
    val positiveStock = if(stockCount < 0){
        stockCount * -1
    }else{
        stockCount
    }
    text = context.getString(R.string.x_item_by_y, positiveStock.toString(), weNames)
}

@BindingAdapter("inventoryType")
fun TextView.getInventoryType(saleUid: Long){
    if(saleUid == 0L){
        text = context.getString(R.string.receive)
    }else{
        text = context.getString(R.string.deliver)
    }
}

@BindingAdapter("textClazzLogStatus")
fun TextView.setTextClazzLogStatus(clazzLog: ClazzLog) {
    text = when (clazzLog.clazzLogStatusFlag) {
        ClazzLog.STATUS_CREATED -> context.getString(R.string.not_recorded)
        ClazzLog.STATUS_HOLIDAY -> "${context.getString(R.string.holiday)} - ${clazzLog.cancellationNote}"
        ClazzLog.STATUS_RECORDED -> context.getString(R.string.present_late_absent,
                clazzLog.clazzLogNumPresent, clazzLog.clazzLogNumPartial, clazzLog.clazzLogNumAbsent)
        else -> ""
    }
}

private val klockDateFormat: KlockDateFormat by lazy { KlockDateFormat("EEE") }

@BindingAdapter("textShortDayOfWeek")
fun TextView.setTextShortDayOfWeek(localTime: DateTimeTz) {
    text = klockDateFormat.format(localTime)
}

@SuppressLint("SetTextI18n")
@BindingAdapter(value = ["textLocalDateTime", "textLocalDateTimeZone"])
fun TextView.setTextLocalDayAndTime(time: Long, timeZone: TimeZone) {
    val dateFormat = DateFormat.getMediumDateFormat(context)
    val timeFormat = DateFormat.getTimeFormat(context)
    timeFormat.timeZone = timeZone
    dateFormat.timeZone = timeZone
    text = dateFormat.format(time) + " - " + timeFormat.format(time)
}

@BindingAdapter("textDate")
fun TextView.setDateText(time: Long){
    val dateFormat = DateFormat.getDateFormat(context)
    text = if (time > 0) dateFormat.format(time) else ""
}

@BindingAdapter("htmlText")
fun TextView.setHtmlText(htmlText: String?) {
    text = if(htmlText != null) HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY) else ""
}

@BindingAdapter("fileSize")
fun TextView.setFileSize(fileSize: Long) {
    text = UMFileUtil.formatFileSize(fileSize)
}


@BindingAdapter(value=["responseTextFilled"])
fun TextView.setResponseTextFilled(responseText: String?){
    if(responseText == null || responseText.isEmpty()){
        text = context.getString(R.string.not_answered)
    }else{
        text = responseText
    }
}

@BindingAdapter("memberRoleName")
fun TextView.setMemberRoleName(clazzEnrolment: ClazzEnrolment?) {
    text = clazzEnrolment?.roleToString(context, systemImpl) ?: ""
}

@BindingAdapter("memberEnrolmentOutcomeWithReason")
fun TextView.setMemberEnrolmentOutcome(clazzEnrolment: ClazzEnrolmentWithLeavingReason?){
    text = "${clazzEnrolment?.roleToString(context, systemImpl)} - ${clazzEnrolment?.outcomeToString(context,  systemImpl)}"
}

@BindingAdapter("clazzEnrolmentWithClazzAndOutcome")
fun TextView.setClazzEnrolmentWithClazzAndOutcome(clazzEnrolment: ClazzEnrolmentWithClazz?){
    text = "${clazzEnrolment?.clazz?.clazzName} (${clazzEnrolment?.roleToString(context, systemImpl)}) - ${clazzEnrolment?.outcomeToString(context,  systemImpl)}"
}

@BindingAdapter("showisolang")
fun TextView.setIsoLang(language: Language){
    var isoText = ""
    if(language.iso_639_1_standard?.isNotEmpty() == true){
        isoText += language.iso_639_1_standard
    }
    if(language.iso_639_2_standard?.isNotEmpty() == true){
        isoText += "/${language.iso_639_2_standard}"
    }
    text = isoText
}

@BindingAdapter("saleItemTotal")
fun TextView.setSaleItemTotal(saleItem: SaleItem?) {
    val amount: Float = (saleItem?.saleItemQuantity?:0) * (saleItem?.saleItemPricePerPiece ?: 0F)

    text = "" + amount.toString()
}

@BindingAdapter(value =["productNameWithCount", "localeSet"])
fun TextView.setProductNameWithDeliveryCount(saleItemWithProduct: SaleItemWithProduct?, locale: String){
    text = "" + saleItemWithProduct?.saleItemProduct?.getNameLocale(locale) + " (" +
            saleItemWithProduct?.deliveredCount + " " + context.getString(R.string.in_stock) + " )"
}
@BindingAdapter(value = ["productNameWithItemCount", "localeSet"])
fun TextView.setProductNameWithItemCount(productDeet: ProductDeliveryWithProductAndTransactions?,
                locale: String ){
    text = "" + productDeet?.getNameLocale(locale) + " (" +  productDeet?.numItemsExpected + " " + context.getString(R.string.items) + " )"
}

@BindingAdapter(value =["product", "localeSet"])
fun TextView.setProductNameWithLocale(product: Product?, locale: String){
    text = "" + product?.getNameLocale(locale)
}

@BindingAdapter(value =["category", "localeSet"])
fun TextView.setCategoryNameWithLocale(category: Category?, locale: String){
    text = "" + category?.getNameLocale(locale)
}

@BindingAdapter(value =["productDesc", "localeSet"])
fun TextView.setProductDescWithLocale(product: Product?, locale: String){
    text = "" + product?.getDescLocale(locale)
}


@BindingAdapter("inStockAppend")
fun TextView.setInStockAppend(stock: Int?){
    text = stock.toString() + " " + context.getString(R.string.in_stock)
}

@BindingAdapter(value = ["totalSale", "saleForTotalAfterDiscount"])
fun TextView.setSaleItemAfterDiscountTotal(totalSale : Long, sale: Sale?) {
    text = "" + (totalSale - (sale?.saleDiscount ?: 0L)) + " " + context.getString(R.string.afs)
}

@BindingAdapter("selectedStock")
fun TextView.setSelectedStock(person: PersonWithInventoryItemAndStock?){
    text = person?.inventoryItem?.inventoryItemQuantity.toString()
}

@BindingAdapter(value = ["balanceDueOrdertotal", "balanceDueSaleWithDiscount", "balanceDuePaymentTotal"])
fun TextView.setBalanceDueAfterPaymentAndSaleAndDiscount(totalSale : Long, sale: Sale?, totalPayment: Long) {
    text = "" + (totalSale - (sale?.saleDiscount ?: 0L) - totalPayment) + " " + context.getString(R.string.afs)
}

@BindingAdapter("weTotalSaleValue")
fun TextView.setWeTotalSale(personWithSaleInfo: PersonWithSaleInfo?){
    if(personWithSaleInfo?.personGoldoziType == Person.GOLDOZI_TYPE_LE) {
        text = personWithSaleInfo?.totalSale.toString() + " " + context.getString(R.string.afs) +
                " " + context.getString(R.string.total_sales)
    }else{
        text= ""
    }
}

@BindingAdapter(value = ["setSeekBarMax", "deliveryMode", "newSaleDelivery"])
fun SeekBar.setMaxForWE(personWithInventory: PersonWithInventoryItemAndStock,
                        saleDeliveryMode: Boolean, newSaleDelivery: Boolean){
    if(saleDeliveryMode){
        //Set max to selected if > stock
        if(personWithInventory.selectedStock > personWithInventory.stock){
            max = personWithInventory.selectedStock
        }else {
            max = personWithInventory.stock
        }
    }else{
        max = 100
    }
    isEnabled = newSaleDelivery
}

@BindingAdapter("seekBarProgress")
fun SeekBar.setSeekBarProgress(progressValue: Int){
    var newProgress = progressValue
    if(newProgress < 0){
        newProgress = -1 * progressValue
    }
    progress = newProgress
}


@InverseBindingAdapter(attribute = "seekBarProgress")
fun getSeekBarProgress(seekBar: SeekBar): Int{
    return seekBar.progress.toInt()
}

@BindingAdapter("seekBarProgressAttrChanged")
fun SeekBar.setProgressListener(inverseBindingListener: InverseBindingListener) {
    inverseBindingListener.onChange()

}

@BindingAdapter("rolesAndPermissionsText")
fun TextView.setRolesAndPermissionsText(entityRole: EntityRoleWithNameAndRole){
    val scopeType = when (entityRole.erTableId) {
        School.TABLE_ID -> {
            " (" +context.getString(R.string.school)+ ")"
        }
        Clazz.TABLE_ID -> {
            " (" +context.getString(R.string.clazz) + ")"
        }
        Person.TABLE_ID -> {
            " (" + context.getString(R.string.person) + ")"
        }
        else -> ""
    }

    val fullText =entityRole.entityRoleRole?.roleName +  " @ " +
            entityRole.entityRoleScopeName + scopeType
    text = fullText

}

@BindingAdapter(value=["statementStartDate", "statementEndDate"])
fun TextView.setStatementDate(statementStartDate: Long, statementEndDate: Long){
    if(statementStartDate == 0L){
        text = ""
        return
    }

    val dateFormatter = DateFormat.getDateFormat(context)
    var statementDate = dateFormatter.format(statementStartDate)

    if(statementEndDate != 0L && statementEndDate!= Long.MAX_VALUE){
        val startDate = DateTime(statementStartDate)
        val endDate = DateTime(statementEndDate)
        if(startDate.dayOfYear != endDate.dayOfYear){
            statementDate += " - ${dateFormatter.format(statementEndDate)}"
        }
    }

    text = statementDate

}

@BindingAdapter("shortDateTime")
fun TextView.setShortDateTime(time: Long){
    val dateFormat = DateFormat.getDateFormat(context)
    val timeFormat = DateFormat.getTimeFormat(context)
    text = dateFormat.format(time) + " - " + timeFormat.format(time)
}

@BindingAdapter("durationHoursMins")
fun TextView.setDurationHoursAndMinutes(duration: Long){
    val hours = TimeUnit.MILLISECONDS.toHours(duration).toInt()

    var minutes = TimeUnit.MILLISECONDS.toMinutes(duration)

    var durationString = " "

    if(hours >= 1){
        minutes -= TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration))
        durationString += "${resources.getQuantityString(R.plurals.duration_hours, hours, hours)} "
    }

    durationString += resources.getQuantityString(R.plurals.duration_minutes,
            minutes.toInt(), minutes.toInt())

    text = durationString

}

@BindingAdapter("scorePercentage")
fun TextView.setScorePercentage(scoreProgress: ContentEntryStatementScoreProgress?){
    if(scoreProgress == null){
        return
    }
    // (4/5) * (1 - 20%) = penalty applied to score
    text = "${scoreProgress.calculateScoreWithPenalty()}%"
}

@BindingAdapter("durationMinsSecs")
fun TextView.setDurationMinutesAndSeconds(duration: Long){
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration).toInt()

    var seconds = TimeUnit.MILLISECONDS.toSeconds(duration)

    var durationString = " "

    if(minutes >= 1){
        seconds -= TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        durationString += "${resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)} "
    }

    durationString += resources.getQuantityString(R.plurals.duration_seconds,
            seconds.toInt(), seconds.toInt())

    text = durationString

}

@BindingAdapter("statementQuestionAnswer")
fun TextView.setStatementQuestionAnswer(statementEntity: StatementEntity){
    if(statementEntity.statementVerbUid != VERB_ANSWERED_UID){
        visibility = View.GONE
        return
    }else{
        visibility = View.VISIBLE
    }
    val fullStatementJson = statementEntity.fullStatement ?: return
    val statement = gson.fromJson(fullStatementJson, Statement::class.java)
    var statementText = statement?.`object`?.definition?.description?.get("en-US")
    val answerResponse = statement.result?.response
    if(answerResponse?.isNotEmpty() == true || answerResponse?.contains("[,]") == true){
        val responses = answerResponse.split("[,]")
        val choiceMap = statement.`object`?.definition?.choices
        val sourceMap = statement?.`object`?.definition?.source
        val targetMap = statement?.`object`?.definition?.target
        statementText += "<br />"
        responses.forEachIndexed { i, it ->

            var description = choiceMap?.find { choice -> choice.id == it }?.description?.get("en-US")
            if(it.contains("[.]")){
                val dragResponse = it.split("[.]")
                description = ""
                description += sourceMap?.find { source -> source.id == dragResponse[0] }?.description?.get("en-US")
                description += " on "
                description += targetMap?.find { target -> target.id == dragResponse[1] }?.description?.get("en-US")
            }


            statementText += "${i+1}: ${if(description.isNullOrEmpty()) it else description} <br />"


        }

    }
    text = HtmlCompat.fromHtml(statementText ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY)


}


@BindingAdapter("isContentComplete")
fun TextView.setContentComplete(person: PersonWithSessionsDisplay){
    text = if(person.resultComplete){
        when(person.resultSuccess){
            StatementEntity.RESULT_SUCCESS -> {
                context.getString(R.string.passed)
            }
            StatementEntity.RESULT_FAILURE -> {
                context.getString(R.string.failed)
            }
            StatementEntity.RESULT_UNSET ->{
                context.getString(R.string.completed)
            }else ->{
                ""
            }
        }
    }else{
        context.getString(R.string.incomplete)
    } + " - "
}