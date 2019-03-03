package com.ustadmobile.core.db.dao;

import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.util.UMCalendarUtil;
import com.ustadmobile.lib.database.annotation.UmDao;
import com.ustadmobile.lib.database.annotation.UmInsert;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.database.annotation.UmRepository;
import com.ustadmobile.lib.database.annotation.UmUpdate;
import com.ustadmobile.lib.db.entities.ClazzLog;
import com.ustadmobile.lib.db.entities.ClazzWithTimeZone;
import com.ustadmobile.lib.db.entities.Schedule;
import com.ustadmobile.lib.db.entities.ScheduledCheck;
import com.ustadmobile.lib.db.sync.dao.SyncableDao;

import java.util.Calendar;
import java.util.List;

@UmDao(inheritPermissionFrom = ClazzDao.class,
inheritPermissionForeignKey = "scheduleClazzUid",
inheritPermissionJoinedPrimaryKey = "clazzUid")
@UmRepository
public abstract class ScheduleDao implements SyncableDao<Schedule, ScheduleDao> {

    @UmInsert
    public abstract long insert(Schedule entity);

    @UmUpdate
    public abstract void update(Schedule entity);

    @UmInsert
    public abstract void insertAsync(Schedule entity, UmCallback<Long> resultObject);

    @UmQuery("SELECT * FROM Schedule")
    public abstract UmProvider<Schedule> findAllSchedules();

    @UmQuery("SELECT * FROM SCHEDULE")
    public abstract List<Schedule> findAllSchedulesAsList();

    @UmUpdate
    public abstract void updateAsync(Schedule entity, UmCallback<Integer> resultObject);

    @UmQuery("SELECT * FROM Schedule WHERE scheduleUid = :uid")
    public abstract Schedule findByUid(long uid);

    @UmQuery("SELECT * FROM Schedule WHERE scheduleUid = :uid")
    public abstract void findByUidAsync(long uid, UmCallback<Schedule> resultObject);

    @UmQuery("SELECT * FROM Schedule WHERE scheduleClazzUid = :clazzUid AND scheduleActive = 1")
    public abstract UmProvider<Schedule> findAllSchedulesByClazzUid(long clazzUid);

    @UmQuery("SELECT * FROM Schedule WHERE scheduleClazzUid = :clazzUid AND scheduleActive = 1")
    public abstract List<Schedule> findAllSchedulesByClazzUidAsList(long clazzUid);

    public void disableSchedule(long scheduleUid){
        findByUidAsync(scheduleUid, new UmCallback<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                result.setScheduleActive(false);
                update(result);
            }

            @Override
            public void onFailure(Throwable exception) {

            }
        });
    }

    /**
     *
     * @param startTime
     * @param endTime
     * @param accountPersonUid
     * @param db
     */
    public void createClazzLogs(long startTime, long endTime, long accountPersonUid, UmAppDatabase db) {
        //This method will usually be called from the Workmanager in Android every day. Making the
        // start time 00:00 and end tim 23:59
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTimeInMillis(startTime);
        UMCalendarUtil.normalizeSecondsAndMillis(startCalendar);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(endTime);
        UMCalendarUtil.normalizeSecondsAndMillis(endCalendar);

        long startMsOfDay = ((startCalendar.get(Calendar.HOUR_OF_DAY) * 24) +
                startCalendar.get(Calendar.MINUTE)) * 60 * 1000;

        List<ClazzWithTimeZone> clazzList = db.getClazzDao().findAllClazzesWithSelectPermission(
                accountPersonUid);
        for(ClazzWithTimeZone clazz : clazzList) {
            if(clazz.getTimeZone() == null) {
                System.err.println("Warning: cannot create schedules for clazz" +
                        clazz.getClazzUid() + " as it has no timezone");
                continue;
            }

            //TODOne: Check if this works for Scheduling for everyday
            //TODO: Check/Test it
            List<Schedule> clazzSchedules = findAllSchedulesByClazzUidAsList(clazz.getClazzUid());
            for(Schedule schedule : clazzSchedules) {

                boolean incToday = startMsOfDay <= schedule.getSceduleStartTime();
                long startTimeMins = schedule.getSceduleStartTime() / (1000 * 60);

                Calendar nextScheduleOccurence = null;

                if(schedule.getScheduleFrequency() == Schedule.SCHEDULE_FREQUENCY_DAILY){

                    //TODO: Skip if its a weekend. TODO: Add weekend feature.
                    //Everyday- so today.
                    nextScheduleOccurence = UMCalendarUtil.copyCalendarAndAdvanceTo(
                            startCalendar, clazz.getTimeZone(),
                            Calendar.getInstance().get(Calendar.DAY_OF_WEEK), incToday);
                    nextScheduleOccurence.set(Calendar.HOUR_OF_DAY, (int) (startTimeMins / 60));
                    nextScheduleOccurence.set(Calendar.MINUTE, (int) (startTimeMins % 60));
                    nextScheduleOccurence.set(Calendar.SECOND, 0);
                    nextScheduleOccurence.set(Calendar.MILLISECOND, 0);

                }else if(schedule.getScheduleFrequency() == Schedule.SCHEDULE_FREQUENCY_WEEKLY) {

                    nextScheduleOccurence = UMCalendarUtil.copyCalendarAndAdvanceTo(
                            startCalendar, clazz.getTimeZone(), schedule.getScheduleDay(), incToday);
                    nextScheduleOccurence.set(Calendar.HOUR_OF_DAY, (int) (startTimeMins / 60));
                    nextScheduleOccurence.set(Calendar.MINUTE, (int) (startTimeMins % 60));
                    nextScheduleOccurence.set(Calendar.SECOND, 0);
                    nextScheduleOccurence.set(Calendar.MILLISECOND, 0);
                }

                if (nextScheduleOccurence != null && nextScheduleOccurence.before(endCalendar)) {
                    //this represents an instance of this class that should take place and
                    //according to the arguments provided, we should check that this instance exists
                    int logInstanceHash = ClazzLogDao.generateClazzLogUid(clazz.getClazzUid(),
                            nextScheduleOccurence.getTimeInMillis());
                    ClazzLog existingLog = db.getClazzLogDao().findByUid(logInstanceHash);

                    if (existingLog == null || existingLog.isCanceled()) {
                        ClazzLog newLog = new ClazzLog(logInstanceHash, clazz.getClazzUid(),
                                nextScheduleOccurence.getTimeInMillis(), schedule.getScheduleUid());
                        db.getClazzLogDao().replace(newLog);
                    }
                }

            }
        }
    }

    @UmInsert
    public abstract void insertScheduledCheck(ScheduledCheck check);


    public void createClazzLogsForEveryDayFromDays(int days, long accountPersonUid,
                                                   UmAppDatabase db){
        for(int i=1;i<=days;i++){
            Calendar dayCal = Calendar.getInstance();
            dayCal.add(Calendar.DATE, -i);
            dayCal.set(Calendar.HOUR_OF_DAY,0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            dayCal.set(Calendar.MILLISECOND, 0);
            long startTime = dayCal.getTimeInMillis();

            dayCal.set(Calendar.HOUR_OF_DAY, 23);
            dayCal.set(Calendar.MINUTE, 59);
            dayCal.set(Calendar.SECOND, 59);
            dayCal.set(Calendar.MILLISECOND, 999);
            long endTime = dayCal.getTimeInMillis();
            createClazzLogs(startTime, endTime, accountPersonUid, db);
        }
    }
    /**
     *  Creates clazzLog for today since clazzlogs are generated for the next day
     *  automatically.
     */
    public void createClazzLogsForToday(long accountPersonUid, UmAppDatabase db) {
        Calendar dayCal = Calendar.getInstance();
        dayCal.set(Calendar.HOUR_OF_DAY, 0);
        dayCal.set(Calendar.MINUTE, 0);
        dayCal.set(Calendar.SECOND, 0);
        dayCal.set(Calendar.MILLISECOND, 0);
        long startTime = dayCal.getTimeInMillis();

        dayCal.set(Calendar.HOUR_OF_DAY, 23);
        dayCal.set(Calendar.MINUTE, 59);
        dayCal.set(Calendar.SECOND, 59);
        dayCal.set(Calendar.MILLISECOND, 999);
        long endTime = dayCal.getTimeInMillis();
        createClazzLogs(startTime, endTime, accountPersonUid, db);
    }

}
