package kz.diaspora.app.ui.sign_in

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat.recreate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kz.diaspora.app.R
import kz.diaspora.app.SelectCountryFragment
import kz.diaspora.app.databinding.FragmentSignInBinding
import kz.diaspora.app.ui.MainActivity
import kz.diaspora.app.ui.StartActivity
import kz.diaspora.app.ui.forgot_password.ForgotPasswordFragment
import kz.diaspora.app.ui.sign_up.SignUpFragment
import kz.my_portfel.app.ui.pincode.DropPasswordFragment
import java.util.*


@AndroidEntryPoint
class SignInFragment : Fragment() {

    private val TAG: String = this::class.java.simpleName

    private val viewModel: SignInViewModel by viewModels()
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        _binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_sign_in, container, false
        )
        setHasOptionsMenu(true)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setObservers()
        setListeners()

    }


    private fun initView() {

    }

    private fun setObservers() {
        with(viewModel) {
            error.observe(viewLifecycleOwner, {
                Toast.makeText(context, "Вы ввели не правильный пароль или логин", Toast.LENGTH_LONG).show()
            })

            loginData.observe(viewLifecycleOwner, {
                if (it.access_token.isNotEmpty() && it.user.city_id != null ) {
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    (activity as StartActivity).finish()
                }else if(it.user.city_id == null){
                    (activity as StartActivity).addFragment(SelectCountryFragment())
//                    Toast.makeText(context, "Выберите откуда вы и где вы находитесь", Toast.LENGTH_LONG).show()
                }
            })
        }

    }

    private fun setListeners() {
        binding.btnEnter.setOnClickListener {
            viewModel.login(binding.etLogin.text.toString(), binding.etPassword.text.toString())
        }
        binding.btnForgotPassword.setOnClickListener {
            (activity as StartActivity).addFragment(ForgotPasswordFragment())
        }
        binding.btnCreateAccount.setOnClickListener {
            (activity as StartActivity).addFragment(SignUpFragment())
        }

    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_auth, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_additional) {
            val fragment = AboutBSFragment()
            fragment.show(childFragmentManager, "AboutBSFragment")
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
