package com.example.change;


import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.change.model.CafeItem;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.valueOf;


/**
 * A simple {@link Fragment} subclass.
 */
public class MenuListFragment extends Fragment {

// 시현 코드

    // 자기자신 - 싱글톤
    public static MenuListFragment fragment = new MenuListFragment();


    public static MenuListFragment getFragment(){
        return fragment;
    }
    //end method



    public MenuListFragment() {
        // Required empty public constructor
    }
    //end 생성자

    RecyclerView recyclerView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_menu_list, container, false);

        recyclerView=(RecyclerView) view.findViewById(R.id.recyclerView);

        citems=new ArrayList<CafeItem>();

        // storage
        mStorageRef= FirebaseStorage.getInstance().getReference("Image");



        // 리사이클러뷰


        //어댑터 만들기  // 클릭하면 이동 ?
        adapter=new CafeItemAdapter(new CafeItemAdapter.OnCafeItemClickListener() {
            @Override
            public void onCafeItemClicked(CafeItem model) {
                Toast.makeText(getActivity(), model.getName()+" 상세정보 보기", Toast.LENGTH_SHORT).show();
/*
                Intent intent = new Intent(MenuListActivity.this, DetailMenuItemActivity.class);
                intent.putExtra("detail",model);
                ArrayList<CafeItem> shoplist=(ArrayList<CafeItem>) getIntent().getSerializableExtra("shoplist");
                // 시현: 음.. 어디서 오는걸까 getIntent ? < payment list fragment 에서 온다

                intent.putExtra("shoplist",shoplist); // 지연 : 고대로 보냄
                startActivity(intent);
*/

                // 시현 acivity : intent > fragment 간 이동으로 임시 변경
                DetailMenuItemFragment fragment = DetailMenuItemFragment.getFragment();
                fragment.setCafeItem(model);
                fragment.setCafeItemArrayList((ArrayList<CafeItem>) citems); // arrayList ? payment list fragment 에서 온다

                getActivity().getSupportFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.body, fragment)
                        .commit();


            }
        });
        recyclerView.setAdapter(adapter);
        Log.d("현재",citems.size()+"");


        // 데이터베이스 읽기 #2. Single ValueEventListener
        FirebaseDatabase.getInstance().getReference().child("menu").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Log.d("MainActivity", "Single ValueEventListener : " + snapshot.getValue());
                    Log.d("데이터",valueOf(snapshot)+"형");
                    String name = (String) snapshot.child("name").getValue();
                    Long pricen = (Long) snapshot.child("price").getValue();
                    String body=(String)snapshot.child("body").getValue();
                    int price=pricen.intValue();
                    //int price=100000;
                    String imageUrl=(String)snapshot.child("imageUrl").getValue();

                    // 객체 형태로 받아와야 함. 오류...
                    //CafeItem ciObject = dataSnapshot.getValue(CafeItem.class);
                    citems.add(new CafeItem(name,price,imageUrl,body));
                    Log.d("현재 들어감",citems.size()+"");


                }
                // for문 다 수행 후 어댑터 설정
                adapter.setItems(citems);


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        // 파이어베이스 입출력 : 출처: https://stack07142.tistory.com/282 [Hello World]

        return view;
    }
    //end on create view

//    ArrayList<CafeItem> cafeItemArrayList;
    public void setCafeItemArrayList(ArrayList<CafeItem> cafeItemArrayList){
        citems = cafeItemArrayList;
    }
    //end method



// 지연 코드

    private static final int RESULT_OK = 3;

    List<CafeItem> citems; // 메뉴판 객체들을 담은 arraylist
    CafeItemAdapter adapter;
    public Uri imguri;
    Button ch,up,down; // 메뉴 사진 등록을 위한 변수.
    ImageView img;
    StorageReference mStorageRef;
    StorageReference ref;// 다운로드 시도
    private StorageTask uploadTask; //중복 방지
    Button button;
    int cnt=0;

    // 이미지 다운로드 하기
    public void download(){

    }
    // 앨범에서 파일 고르기
    private void fileChooser(){
        Intent intent=new Intent();
        intent.setType("image/'");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,1);
    }


    private String getExtension(Uri uri){
        ContentResolver cr=getActivity().getContentResolver();
        MimeTypeMap mimeTypeMap=MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cr.getType(uri));
    }
    // 이미지를 파이어베이스에 올림
    private void fileUploader(){
        StorageReference Ref=mStorageRef.child(System.currentTimeMillis()+","+getExtension(imguri));
        uploadTask=Ref.putFile(imguri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        // 성공하면
                        Toast.makeText(getActivity(),"Image Uproad Successfultty",Toast.LENGTH_LONG).show();

                        Log.d("백지연",taskSnapshot.getMetadata().getReference().getDownloadUrl()+"");
                        taskSnapshot.getMetadata().getReference().getDownloadUrl()
                                .addOnCompleteListener(getActivity(),
                                        new OnCompleteListener<Uri>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Uri> task) {
                                                if (task.isSuccessful()) {
                                                    CafeItem ci = new CafeItem("변경필요", 5000, task.getResult().toString(),"상세설명");
                                                    //mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(key).setValue(friendlyMessage);
                                                    FirebaseDatabase.getInstance().getReference().child("menu").push().setValue(ci);
                                                }
                                            }
                                        });
                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==1&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            imguri=data.getData();
            img.setImageURI(imguri);
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_menu_list);  // fragment 라서..

    }// onCreate()
    // 리사이클러뷰 어댑터 클래스
    private static class CafeItemAdapter extends RecyclerView.Adapter<CafeItemAdapter.CafeItemViewHolder> {
        interface OnCafeItemClickListener {
            void onCafeItemClicked(CafeItem model);
        }

        private OnCafeItemClickListener mListener;

        private List<CafeItem> mItems = new ArrayList<>();

        public CafeItemAdapter() {}

        public CafeItemAdapter(OnCafeItemClickListener listener) {
            mListener = listener;
        }

        public void setItems(List<CafeItem> items) {
            this.mItems = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CafeItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cafeitem, parent, false);
            final CafeItemViewHolder viewHolder = new CafeItemViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        final CafeItem item = mItems.get(viewHolder.getAdapterPosition());
                        mListener.onCafeItemClicked(item);
                    }
                }
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull CafeItemViewHolder holder, int position) {
            CafeItem item = mItems.get(position);
            // TODO : 데이터를 뷰홀더에 표시하시오
            holder.name.setText(item.getName());
            holder.price.setText(item.getPrice()+"");
            // holder.cafe_imageview.setImageResource();
            Glide.with(holder.cafe_imageview.getContext())
                    .load(item.getImageUrl())
                    .into(holder.cafe_imageview);
            //String imageUrl = friendlyMessage.getImageUrl();
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        public static class CafeItemViewHolder extends RecyclerView.ViewHolder {
            // TODO : 뷰홀더 완성하시오
            TextView name;
            TextView price;
            ImageView cafe_imageview;

            public CafeItemViewHolder(@NonNull View itemView) {
                super(itemView);
                // TODO : 뷰홀더 완성하시오
                name=itemView.findViewById(R.id.name_text);
                price=itemView.findViewById(R.id.age_text);
                cafe_imageview=itemView.findViewById(R.id.cafe_imageview);
            }
        }
    }//CafeItemAdapter 클래스


}
