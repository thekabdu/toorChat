package kz.diaspora.app.ui.messages

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kz.diaspora.app.R
import kz.diaspora.app.databinding.FragmentMessagesBinding
import kz.diaspora.app.domain.model.ChatModel
import kz.diaspora.app.domain.model.MessageModel
import kz.diaspora.app.domain.model.NotificationData
import kz.diaspora.app.domain.model.PushNotificationModel
import kz.diaspora.app.ui.MainActivity
import kz.diaspora.app.ui.messages.adapter.MessagesAdapter
import org.json.JSONException
import org.json.JSONObject
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.MediaSource
import java.io.File


@AndroidEntryPoint
class MessagesFragment : Fragment(), MessagesAdapter.OnMessageClickListener {

    private val TAG: String = this::class.java.simpleName

    private val args: MessagesFragmentArgs by navArgs()
    private val viewModel: MessagesViewModel by viewModels()
    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private var socket: Socket? = null
    val gson: Gson = Gson()

    private val adapter: MessagesAdapter by lazy { MessagesAdapter(arrayListOf()) }
   // val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_container)

    private lateinit var easyImage: EasyImage
    private var avatarFile: File? = null

    companion object {

        fun newInstance(): Fragment {
            val fragment = MessagesFragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_messages, container, false
        )
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setObservers()
        setListeners()
        initSocket()
        viewModel.getMessages(args.model.id.toString())

        binding.swipe.setOnRefreshListener {
            refresh()
        }

        (activity as AppCompatActivity).supportActionBar?.title = viewModel.messagesData.value?.chat_name


    }

    private fun initView() {
        val mLayoutManager = LinearLayoutManager(context)
        mLayoutManager.stackFromEnd = true
        mLayoutManager.reverseLayout = false
        binding.rvMainHome.layoutManager = mLayoutManager
        binding.rvMainHome.adapter = adapter
    }

    private fun initSocket() {
        val room = mutableListOf<String>()
        args.model.chat_room?.let { room.add(it) }
        println("rooms " + room)

        try {
            socket = IO.socket("http://45.147.197.44/")
        } catch (e: Exception) {
            e.printStackTrace()
            println("connect")
        }

        socket!!.connect()
        socket!!.emit("join_group", room)

        socket!!.on("group_chat", onNewMessage)
        socket!!.connect()

    }

    private val onNewMessage = Emitter.Listener { args ->
        Toast.makeText(context, "Received", Toast.LENGTH_LONG).show()
        requireActivity().runOnUiThread(Runnable {
            val data = args[0] as JSONObject
            val username: String
            val message: String
            try {
                username = data.getString("username")
                message = data.getString("message")
            } catch (e: JSONException) {
                return@Runnable
            }
        })
    }

    private fun setObservers() {
        with(viewModel) {
            messageData.observe(viewLifecycleOwner, {
                adapter.clear()
                adapter.add(it)
                (activity as AppCompatActivity).supportActionBar?.title = ""
                args.model.chat_name?.let { it1 -> (activity as MainActivity).setToolbarTitle(it1) }
                (activity as MainActivity).setToolbarEndText(args.model.chat_users_count.toString())



                (activity as MainActivity).setToolbarLike(true)
                (activity as MainActivity).setToolbarImage(args.model.is_liked)
            })
            error.observe(viewLifecycleOwner, {
//                Toast.makeText(context, "${it?.error}", Toast.LENGTH_LONG).show()
            })
        }
    }

    private fun setListeners() {
        binding.ivAddFiles.setOnClickListener {
            easyImage = EasyImage.Builder(requireContext())
                .setCopyImagesToPublicGalleryFolder(true)
                .setFolderName("diaspora")
                .allowMultiple(false)
                .build()
            easyImage.openChooser(this)
        }
        binding.ivSendMessage.setOnClickListener {
            if (binding.etMessage.text.toString() != "") {
                viewModel.sendMessage(args.model.id.toString(), binding.etMessage.text.toString())
                binding.etMessage.setText("")
            }
        }

        /*binding.ivSendMessage.setOnClickListener {
            val username = binding.etMessage.text.toString()
            val message = binding.etMessage.text.toString()
            val recipientToken = binding.etToken.text.toString()
            if(username.isNotEmpty() && message.isNotEmpty() && recipientToken.isNotEmpty()) {
                PushNotificationModel(
                    NotificationData(username, message),
                    recipientToken
                ).also {
                    viewModel.sendNotification(it)
                }
            }
        }*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        easyImage.handleActivityResult(
            requestCode,
            resultCode,
            data,
            requireActivity(),
            object : DefaultCallback() {
                override fun onMediaFilesPicked(imageFiles: Array<MediaFile>, source: MediaSource) {
                    avatarFile = imageFiles[0].file
//                    Glide.with(requireContext())
//                        .load(imageFiles[0].file)
//                        .circleCrop()
//                        .into(binding.ivAvatar)

                }

                override fun onImagePickerError(error: Throwable, source: MediaSource) {
                    //Some error handling
                    error.printStackTrace()
                }

                override fun onCanceled(source: MediaSource) {
                    //Not necessary to remove any files manually anymore
                }
            })
    }

    override fun onMessageClick(messageModel: MessageModel) {

    }

   /* private fun onClick(v: View){
        val action = MessagesFragmentDirections.toUsersInChatFragment()
        v.findNavController().navigate(action)
    }*/

   /* override fun onOptionsItemSelected(item: MenuItem,chatModel: ChatModel): Boolean {
         if(item.itemId == R.id.tv_drop) {
            val action = MessagesFragmentDirections.toUsersInChatFragment(chatModel)
            findNavController().navigate(action)
        }
        return true
    }*/

    private fun refresh() {
        adapter.clear()
        viewModel.getMessages(args.model.id.toString())
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}