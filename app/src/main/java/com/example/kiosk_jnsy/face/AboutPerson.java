package com.example.kiosk_jnsy.face;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

//import com.example.change.Camera2BasicFragment;
//import com.example.change.model.UserDTO;
//import com.example.change.setting.AppSetting;
import com.example.kiosk_jnsy.CameraActivity;
import com.example.kiosk_jnsy.MainActivity;
import com.example.kiosk_jnsy.model.UserDTO;
import com.example.kiosk_jnsy.setting.AppSetting;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.Person;

import java.io.InputStream;
import java.util.UUID;

//import com.example.camera2basic.Camera2BasicFragment;
//import com.example.camera2basic.model.UserDTO;
//import com.example.camera2basic.setting.AppSetting;

//import com.example.camera.setting.AppSetting;

public class AboutPerson {

    static Handler handler = new Handler(Looper.getMainLooper());

    //ListPersonTask
    public static class ListPersonTask extends AsyncTask<Void, Void, Person[]> {
        Fragment fragment;
        Activity activity;
        // 생성자
        public ListPersonTask(Fragment fragment){
            this.fragment=fragment;
        }
        @Override
        protected Person[] doInBackground(Void... voids) {

            try{

                return AppSetting.faceServiceClient.listPersonsInLargePersonGroup(AppSetting.personGroupId);

            }catch (Exception e){
                Log.e("   error","listPersonGroup");
                return null;
            }

        }

        @Override
        protected void onPostExecute(Person[] Persons) {
            super.onPostExecute(Persons);

            if(Persons == null){
                return;
            }

            String str = Persons.length+"  ";
            // 성공하면
            for(int i=0;i<Persons.length;i++){
                str += Persons[i].name+" "+Persons[i].personId;
            }

            final String strr = str;
            handler.postDelayed(new Runnable() { // handler 에 looper 할당안하면 여기서 오류
                @Override
                public void run() {
                    ((CameraActivity)activity).addTextToEditText(strr+"\n");
                }
            },0);

        }
    }
    //end task

    public static class CreatePersonTask extends AsyncTask<String, String, UUID> {

        String personName;
        InputStream inputStream;
        Face face;
        Fragment fragment;
        Activity activity;

        public CreatePersonTask(Fragment fragment){
            this.fragment=fragment;
        }

        public CreatePersonTask(String name, Fragment fragment){
            personName = name;
            this.fragment=fragment;
        }
        public CreatePersonTask(String name, Activity activity){
            personName = name;
            this.activity=activity;
        }

        public CreatePersonTask(String name, InputStream inputStream, Face face){
            personName = name;
            this.face = face;
            this.inputStream = inputStream;
        }

        public CreatePersonTask(String name, InputStream inputStream, Face face, Fragment fragment){
            personName = name;
            this.face = face;
            this.inputStream = inputStream;
            this.fragment=fragment;
        }

        @Override
        protected UUID doInBackground(String... params) {

            try{

                // create person
                CreatePersonResult createPersonResult = AppSetting.faceServiceClient.createPersonInLargePersonGroup(
                        AppSetting.personGroupId, // 그룹id
                        personName, // name
                        null); // userData
                // Create Person Result 는 멤버변수 UUID 하나 갖는 클래스

                return createPersonResult.personId;

            } catch (Exception e) {
                Log.e("   error", e.getMessage()); // 오류 로그 찍는다

/* 지우지마요
                handler.postDelayed(new Runnable() { // handler 에 looper 할당안하면 여기서 오류
                    @Override
                    public void run() {
                        ((Camera2BasicFragment)fragment).addTextToEditText("create person try-catch");
                    }
                },0);
*/
                return null;
            }
        }

        @Override
        protected void onPostExecute(final UUID personId) {

            if (personId == null) {
                return;  // 오류나면 그냥 종료한다
            }//end if

            UserDTO user = new UserDTO(personName,personId.toString());

            // 객체 DB에 저장 user 밑에 personName, personId
            FirebaseFirestore.getInstance().collection("user")
                    .add(user);

            Log.e("   create person", "");

/* 지우지마요
            handler.postDelayed(new Runnable() { // handler 에 looper 할당안하면 여기서 오류
                @Override
                public void run() {
                    ((Camera2BasicFragment)fragment).addTextToEditText("create person 성공");
                }
            },0);
*/
            AppSetting.personUUID = personId.toString(); // 전역으로 저장
//            AppSetting.isSetPersonUUID = true;
            ((MainActivity)activity).intentToCameraActivity();

            // 사진 10장 추가 // 함수로 스레드 기동 위임
//            ((Camera2BasicFragment)fragment).startCaptureThread();

        }
        //end post
    }
    // end create persony

}//end class
