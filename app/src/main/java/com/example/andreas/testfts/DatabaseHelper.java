package com.example.andreas.testfts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;


/**
 * Created by andreas on 7/22/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper instance;
    private static final String TEST_FTS_DB = "testFts";

    private Scheduler dbScheduler = Schedulers.from(Executors.newSingleThreadExecutor());

    private DatabaseHelper(Context context) {
        super(context, TEST_FTS_DB, null, 3);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE VIRTUAL TABLE fts_user USING fts4(content=\"user\",full_name TEXT)");
        sqLiteDatabase.execSQL("CREATE TABLE user (id INTEGER PRIMARY KEY, full_name TEXT, status VARCHAR(255))");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public Observable<Object> insertUser(final String fullName, final String status) {
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("full_name", fullName);
                contentValues.put("status", status);

                long rowId = getWritableDatabase().insert("user", null, contentValues);

                ContentValues ftsValues = new ContentValues();
                ftsValues.put("docid", rowId);
                ftsValues.put("full_name", fullName);
                getWritableDatabase().insert("fts_user", null, ftsValues);

                subscriber.onNext(rowId);
                subscriber.onCompleted();
            }
        }).subscribeOn(dbScheduler);
    }

    public Observable<List<UserResult>> getUserByFullName(final String fullName) {
        return Observable.create(new Observable.OnSubscribe<List<UserResult>>() {
            @Override
            public void call(Subscriber<? super List<UserResult>> subscriber) {

                try {
                    Cursor cursor;
                    if (fullName.isEmpty()) {
                        cursor = getReadableDatabase().rawQuery(
                                "SELECT rowid, full_name, '' as offset FROM user LIMIT 100", null);
                    } else {
                        cursor = getReadableDatabase().rawQuery(
                                "SELECT docid, full_name, offsets(fts_user) FROM fts_user WHERE fts_user MATCH ? LIMIT 100",
                                new String[]{fullName});
                    }

                    List<UserResult> list = new ArrayList<UserResult>();

                    while (cursor.moveToNext()) {
                        UserResult userResult = new UserResult();
                        userResult.fullName = cursor.getString(1);
                        userResult.offsets = createOffsets(cursor.getString(2), fullName);

                        list.add(userResult);
                    }
                    subscriber.onNext(list);
                } catch (Exception ex) {
                   subscriber.onError(ex);
                }

                subscriber.onCompleted();
            }
        }).subscribeOn(dbScheduler);
    }

    private Offset[] createOffsets(String offsets, String fullName) {
        if (offsets.isEmpty()) {
            return null;
        }

        final String[] fullNameToken = fullName.split(" ");

        return Observable.from(offsets.split(" "))
                .map(new Func1<String, Integer>() {
                    @Override
                    public Integer call(String elem) {
                        return Integer.parseInt(elem);
                    }
                })
                .buffer(4)
                .map(new Func1<List<Integer>, Offset>() {
                    @Override
                    public Offset call(List<Integer> integers) {
                        Offset offset = new Offset();

                        int termIndex = integers.get(1);
                        offset.start = integers.get(2);
                        offset.end = offset.start
                                + fullNameToken[termIndex].length()
                                - 1; // perlu - 1 karena ada tanda * di setiap term

                        return offset;
                    }
                })
                .reduce(new ArrayList<Offset>(), new Func2<List<Offset>, Offset, List<Offset>>() {
                    @Override
                    public List<Offset> call(List<Offset> offsets, Offset offset) {
                        offsets.add(offset);
                        return offsets;
                    }
                })
                .map(new Func1<List<Offset>, Offset[]>() {
                    @Override
                    public Offset[] call(List<Offset> offsets) {
                        return offsets.toArray(new Offset[offsets.size()]);
                    }
                })
                .toBlocking()
                .first();
    }
}
