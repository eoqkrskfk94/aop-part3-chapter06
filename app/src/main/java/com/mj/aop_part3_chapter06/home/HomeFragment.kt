package com.mj.aop_part3_chapter06.home

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.mj.aop_part3_chapter06.DBKey.Companion.CHILD_CHAT
import com.mj.aop_part3_chapter06.DBKey.Companion.DB_ARTICLES
import com.mj.aop_part3_chapter06.DBKey.Companion.DB_USERS
import com.mj.aop_part3_chapter06.R
import com.mj.aop_part3_chapter06.chatlist.ChatListItem
import com.mj.aop_part3_chapter06.databinding.FragmentHomeBinding


class HomeFragment : Fragment(R.layout.fragment_home) {

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    private val articleList = mutableListOf<ArticleModel>()
    private val listener = object: ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val articleModel = snapshot.getValue(ArticleModel::class.java)
            articleModel ?: return
            articleList.add(articleModel)
            articleAdapter.submitList(articleList)
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onChildRemoved(snapshot: DataSnapshot) {}

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onCancelled(error: DatabaseError) {}

    }
    private lateinit var articleDB: DatabaseReference
    private lateinit var userDB: DatabaseReference
    private var binding: FragmentHomeBinding? = null
    private lateinit var articleAdapter: ArticleAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentHomeBinding = FragmentHomeBinding.bind(view)
        binding = fragmentHomeBinding

        articleList.clear()
        userDB = Firebase.database.reference.child(DB_USERS)
        articleDB = Firebase.database.reference.child(DB_ARTICLES)

        articleAdapter = ArticleAdapter(onItemClicked = { articleModel ->
            if(auth.currentUser != null) {

                if(auth.currentUser?.uid != articleModel.sellerId) {
                    val chatRoom = ChatListItem(
                            buyerId = auth.currentUser!!.uid,
                            sellerId = articleModel.sellerId,
                            itemTitle = articleModel.title,
                            key = System.currentTimeMillis()
                    )

                    userDB.child(auth.currentUser!!.uid)
                            .child(CHILD_CHAT)
                            .push()
                            .setValue(chatRoom)

                    userDB.child(articleModel.sellerId)
                            .child(CHILD_CHAT)
                            .push()
                            .setValue(chatRoom)

                    Snackbar.make(view, "created chat room", Snackbar.LENGTH_LONG).show()


                } else {
                    Snackbar.make(view, "my book", Snackbar.LENGTH_LONG).show()
                }


            } else {
                Snackbar.make(view, "use after login", Snackbar.LENGTH_LONG).show()
            }


        })

        fragmentHomeBinding.articleRecyclerView.layoutManager = LinearLayoutManager(context)
        fragmentHomeBinding.articleRecyclerView.adapter = articleAdapter

        fragmentHomeBinding.addFloatingButton.setOnClickListener {
            if(auth.currentUser != null) {
                val intent = Intent(requireContext(), ArticleAddActivity::class.java)
                startActivity(intent)
            }
            else {
                Snackbar.make(view, "use after login", Snackbar.LENGTH_LONG).show()
            }
        }

        articleDB.addChildEventListener(listener)
    }

    override fun onResume() {
        super.onResume()
        articleAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        articleDB.removeEventListener(listener)
    }


}