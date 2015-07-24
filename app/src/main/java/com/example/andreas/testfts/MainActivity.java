package com.example.andreas.testfts;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.ajbrown.namemachine.Name;
import org.ajbrown.namemachine.NameGenerator;

import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;


public class MainActivity extends ActionBarActivity {
    private static final int MAX_USER = 10000;

    private DatabaseHelper database;
    private List<UserResult> userResultList = new ArrayList();
    private CustomAdapter adapter;

    private ListView listView;
    private EditText searchText;
    private Button insertButton;
    private ProgressDialog progressDialog;

    private PublishSubject<String> textChangeStream = PublishSubject.create();
    private PublishSubject<String> reloadStream = PublishSubject.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        database = DatabaseHelper.getInstance(this);

        listView = (ListView) findViewById(R.id.result);
        adapter = new CustomAdapter(userResultList, this);
        listView.setAdapter(adapter);

        searchText = (EditText) findViewById(R.id.search);
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                textChangeStream.onNext(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        Observable.combineLatest(
                textChangeStream
                        .startWith("")
                        .map(new Func1<String, String>() {
                            @Override
                            public String call(String text) {
                                if (text.isEmpty()) {
                                    return "";
                                }

                                String[] splitted = text.split(" ");
                                StringBuilder builder = new StringBuilder();

                                for (String token : splitted) {
                                    builder.append(token).append('*').append(' ');
                                }
                                builder.delete(builder.length() - 1, builder.length());
                                return builder.toString();
                            }
                        }),
                reloadStream.startWith(""),
                new Func2<String, String, String>() {
                    @Override
                    public String call(String s, String s2) {
                        return s;
                    }
                })
                .flatMap(new Func1<String, Observable<List<UserResult>>>() {
                    @Override
                    public Observable<List<UserResult>> call(String fullName) {
                        return database.getUserByFullName(fullName);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<UserResult>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println(throwable);
                    }

                    @Override
                    public void onNext(List<UserResult> userResults) {
                        userResultList.clear();
                        userResultList.addAll(userResults);
                        adapter.notifyDataSetChanged();
                    }
                });

        progressDialog = new ProgressDialog(this);

        insertButton = (Button) findViewById(R.id.insert);
        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.setMax(MAX_USER);
                progressDialog.setCancelable(false);
                progressDialog.show();


                Observable.create(new Observable.OnSubscribe<List<Name>>() {
                            @Override
                            public void call(Subscriber<? super List<Name>> subscriber) {
                                NameGenerator generator = new NameGenerator();
                                List<Name> users = generator.generateNames(MAX_USER);

                                subscriber.onNext(users);
                                subscriber.onCompleted();
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .flatMap(new Func1<List<Name>, Observable<Name>>() {
                            @Override
                            public Observable<Name> call(List<Name> names) {
                                return Observable.from(names);
                            }
                        })
                        .flatMap(new Func1<Name, Observable<Object>>() {
                            @Override
                            public Observable<Object> call(Name name) {
                                return database.insertUser(name.getFirstName() + " " + name.getLastName(),
                                        "Hello!");
                            }
                        })
                        .scan(0, new Func2<Integer, Object, Integer>() {
                            @Override
                            public Integer call(Integer integer, Object o) {
                                return integer + 1;
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Integer>() {
                            @Override
                            public void onCompleted() {
                                adapter.notifyDataSetChanged();
                                progressDialog.dismiss();
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(Integer counter) {
                                progressDialog.setProgress(counter);
                            }
                        });
            }
        });
    }

}
