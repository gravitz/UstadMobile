package com.ustadmobile.core.controller;

import java.util.Hashtable;
import java.util.List;

import com.ustadmobile.core.db.dao.SelQuestionDao;
import com.ustadmobile.core.db.dao.SelQuestionSetDao;
import com.ustadmobile.core.db.dao.SelQuestionSetResponseDao;
import com.ustadmobile.core.generated.locale.MessageID;
import com.ustadmobile.core.impl.UmAccountManager;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.view.SELQuestionView;
import com.ustadmobile.core.view.SELRecognitionView;
import com.ustadmobile.core.view.SELSelectConsentView;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;

import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.lib.db.entities.SelQuestion;
import com.ustadmobile.lib.db.entities.SelQuestionSet;
import com.ustadmobile.lib.db.entities.SelQuestionSetResponse;

import static com.ustadmobile.core.view.ClazzListView.ARG_CLAZZ_UID;
import static com.ustadmobile.core.view.PersonDetailView.ARG_PERSON_UID;
import static com.ustadmobile.core.view.SELEditView.ARG_CLAZZMEMBER_UID;
import static com.ustadmobile.core.view.SELEditView.ARG_QUESTION_INDEX_ID;
import static com.ustadmobile.core.view.SELEditView.ARG_QUESTION_SET_RESPONSE_UID;
import static com.ustadmobile.core.view.SELEditView.ARG_QUESTION_SET_UID;
import static com.ustadmobile.core.view.SELEditView.ARG_QUESTION_UID;
import static com.ustadmobile.core.view.SELQuestionView.ARG_QUESTION_INDEX;
import static com.ustadmobile.core.view.SELQuestionView.ARG_QUESTION_TEXT;
import static com.ustadmobile.core.view.SELQuestionView.ARG_QUESTION_TOTAL;
import static com.ustadmobile.core.view.SELRecognitionView.ARG_RECOGNITION_UID;
import static com.ustadmobile.core.view.SELSelectStudentView.ARG_DONE_CLAZZMEMBER_UIDS;
import static com.ustadmobile.core.view.SELSelectStudentView.ARG_SELECTED_QUESTION_SET_UID;


/**
 * The SELSelectConsent Presenter - responsible for the logic in displaying seeking consent from
 * the student/sel officer on behalf of the student - a reminder that we are taking this information.
 *
 */
public class SELSelectConsentPresenter
        extends UstadBaseController<SELSelectConsentView> {

    //Any arguments stored as variables here
    private long currentClazzUid = 0;
    private long currentPersonUid = 0;
    private long currentClazzMemberUid = 0;
    private int MIN_RECOGNITION_SUCCESSES = 0;
    private String doneClazzMemberUids = "";
    private long currentQuestionSetUid = 0;
    public static final int BASE_INDEX_SEL_QUESTION = 0;

    UmAppDatabase repository = UmAccountManager.getRepositoryForActiveAccount(context);

    public SELSelectConsentPresenter(Object context, Hashtable arguments, SELSelectConsentView view) {
        super(context, arguments, view);

        //Get clazz uid and set them.
        if(arguments.containsKey(ARG_CLAZZ_UID)){
            currentClazzUid = (long) arguments.get(ARG_CLAZZ_UID);
        }
        //Get person uid
        if(arguments.containsKey(ARG_PERSON_UID)){
            currentPersonUid = (long) arguments.get(ARG_PERSON_UID);
        }
        //Get clazz member doing the sel
        if(arguments.containsKey(ARG_CLAZZMEMBER_UID)){
            currentClazzMemberUid = (long) arguments.get(ARG_CLAZZMEMBER_UID);
        }
        if(arguments.containsKey(ARG_DONE_CLAZZMEMBER_UIDS)){
            doneClazzMemberUids = (String) arguments.get(ARG_DONE_CLAZZMEMBER_UIDS);
        }
        if(arguments.containsKey(ARG_SELECTED_QUESTION_SET_UID)){
            currentQuestionSetUid = (long) arguments.get(ARG_SELECTED_QUESTION_SET_UID);
        }

    }

    @Override
    public void onCreate(Hashtable savedState) {
        super.onCreate(savedState);
    }

    /**
     * Handles click "START SELECTION". Checks for consent and Gets to display the first question
     * after creating a new SEL run on the database and response.
     *
     * @param consentGiven true if consent given. False if not.
     */
    public void handleClickPrimaryActionButton(boolean consentGiven) {
        SelQuestionSetResponseDao selQuestionSetResponseDao =
                repository.getSocialNominationQuestionSetResponseDao();
        UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();

        //Check selectedObject for consent given.
        if(consentGiven){

            selQuestionSetResponseDao.findAllPassedRecognitionByPersonUid(
                    currentClazzMemberUid,
                    new UmCallback<List<SelQuestionSetResponse>>() {
                @Override
                public void onSuccess(List<SelQuestionSetResponse> listPassed) {

                    if(listPassed.size() > MIN_RECOGNITION_SUCCESSES){
                        //Go straight to the Questions
                        goToNextQuestion();

                    }else{
                        //TODO remove another newResponse creation? : Check
                        //Go re-do/do the recognition activity.
                        SelQuestionSetResponse newResponse =
                                new SelQuestionSetResponse();
                        newResponse.setSelQuestionSetResponseStartTime(System.currentTimeMillis());
                        newResponse.setSelQuestionSetResponseClazzMemberUid(currentClazzMemberUid);
                        newResponse.setSelQuestionSetResposeUid(
                                selQuestionSetResponseDao.insert(newResponse));

                        Hashtable<String, Object> args = new Hashtable<>();
                        args.put(ARG_RECOGNITION_UID, newResponse.getSelQuestionSetResposeUid());
                        args.put(ARG_CLAZZ_UID, currentClazzUid);
                        args.put(ARG_PERSON_UID, currentPersonUid);
                        args.put(ARG_CLAZZMEMBER_UID, currentClazzMemberUid);
                        args.put(ARG_SELECTED_QUESTION_SET_UID, currentQuestionSetUid);
                        doneClazzMemberUids += "," + String.valueOf(currentClazzMemberUid);
                        args.put(ARG_DONE_CLAZZMEMBER_UIDS, doneClazzMemberUids);

                        view.finish();

                        impl.go(SELRecognitionView.VIEW_NAME, args, view.getContext());

                    }
                }

                @Override
                public void onFailure(Throwable exception) {
                    exception.printStackTrace();
                }
            });
        }else {
            //TODOne: Handle and think about what happens if the consent is NOT given.
            //UI: Maybe some toast?
            view.toastMessage(impl.getString(MessageID.consent_not_selected, context));
        }
    }

    /**
     * Method that checks where the current SEL task is in and goes to the next question.
     *
     */
    private void goToNextQuestion(){

        UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
        SelQuestionSetResponseDao selQuestionSetResponseDao =
                repository.getSocialNominationQuestionSetResponseDao();
        SelQuestionSetDao questionSetDao = repository
                .getSocialNominationQuestionSetDao();
        SelQuestionDao questionDao = repository
                .getSocialNominationQuestionDao();

        //Loop through questions.
        questionSetDao.findAllQuestionsAsync(new UmCallback<List<SelQuestionSet>>() {
            @Override
            public void onSuccess(List<SelQuestionSet> questionSets) {

                //Update: Sprint 5: Question Set will be selectable at the
                // SELSelectStudentView screen.
                //TODOne: Change this when we add more Question Sets to
                // findNextQuestionSet like we did for findNextQuestion
                for(SelQuestionSet questionSet : questionSets){

                    //Find total number of questions as well.
                    int totalSELQuestions =
                        questionDao.findTotalNumberOfActiveQuestionsInAQuestionSet(
                                questionSet.getSelQuestionSetUid()
                        );

                    questionDao.findNextQuestionByQuestionSetUidAsync(questionSet.getSelQuestionSetUid(),
                            BASE_INDEX_SEL_QUESTION, new UmCallback<SelQuestion>() {
                        @Override
                        public void onSuccess(SelQuestion nextQuestion) {
                            if(nextQuestion != null) {

                                SelQuestionSetResponse newResponse = new SelQuestionSetResponse();
                                newResponse.setSelQuestionSetResponseStartTime(System.currentTimeMillis());
                                newResponse.setSelQuestionSetResponseSelQuestionSetUid(
                                        questionSet.getSelQuestionSetUid());
                                newResponse.setSelQuestionSetResponseClazzMemberUid(currentClazzMemberUid);

                                selQuestionSetResponseDao.insertAsync(newResponse, new UmCallback<Long>() {
                                    @Override
                                    public void onSuccess(Long questionSetResponseUid) {

                                        view.finish();

                                        //Create arguments
                                        Hashtable<String, Object> args = new Hashtable<>();
                                        args.put(ARG_CLAZZ_UID, currentClazzUid);
                                        args.put(ARG_PERSON_UID, currentPersonUid);
                                        args.put(ARG_QUESTION_SET_UID, questionSet.getSelQuestionSetUid());
                                        args.put(ARG_CLAZZMEMBER_UID, currentClazzMemberUid);
                                        args.put(ARG_QUESTION_UID, nextQuestion.getSelQuestionUid());
                                        args.put(ARG_QUESTION_INDEX_ID, nextQuestion.getQuestionIndex());
                                        args.put(ARG_QUESTION_SET_RESPONSE_UID, questionSetResponseUid);
                                        args.put(ARG_QUESTION_TEXT, nextQuestion.getQuestionText());
                                        args.put(ARG_QUESTION_INDEX, nextQuestion.getQuestionIndex());
                                        args.put(ARG_QUESTION_TOTAL, totalSELQuestions);

                                        impl.go(SELQuestionView.VIEW_NAME, args, view.getContext());

                                    }

                                    @Override
                                    public void onFailure(Throwable exception) {
                                        exception.printStackTrace();
                                    }
                                });

                            }else{
                                //End the SEL activities properly.
                                view.finish();
                            }
                        }

                        @Override
                        public void onFailure(Throwable exception) {
                            exception.printStackTrace();
                        }
                    });
                }

            }

            @Override
            public void onFailure(Throwable exception) {
                exception.printStackTrace();
            }
        });
    }


}
