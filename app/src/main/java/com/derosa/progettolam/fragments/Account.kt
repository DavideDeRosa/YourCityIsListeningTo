package com.derosa.progettolam.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.derosa.progettolam.R
import com.derosa.progettolam.activities.LoginActivity
import com.derosa.progettolam.util.DataSingleton
import com.derosa.progettolam.util.ExtraUtil
import com.derosa.progettolam.viewmodel.UserViewModel


class Account : Fragment() {

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isNetworkAvailable = sharedPref.getBoolean("network_state", false)

        if (!isNetworkAvailable) {
            view.findViewById<TextView>(R.id.txtOfflineAccount).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.txtUsername).visibility = View.GONE
            view.findViewById<TextView>(R.id.txtDescrizioneAccount).visibility = View.GONE
            view.findViewById<Button>(R.id.btnLogout).visibility = View.GONE
            view.findViewById<Button>(R.id.btnUnsub).visibility = View.GONE
        } else {
            val btnUnsub = view.findViewById<Button>(R.id.btnUnsub)
            val btnLogout = view.findViewById<Button>(R.id.btnLogout)
            val txtUsername = view.findViewById<TextView>(R.id.txtUsername)

            val fullText = "Ciao, ${DataSingleton.username}!"
            val spannableString = SpannableString(fullText)
            spannableString.setSpan(
                ForegroundColorSpan(Color.RED),
                "Ciao, ".length,
                fullText.length - 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            txtUsername.text = spannableString

            btnUnsub.setOnClickListener {
                val token = DataSingleton.token
                if (token != null) {
                    showUnsubscribeConfirmationDialog(token)
                } else {
                    goToLogin()
                }
            }

            btnLogout.setOnClickListener {
                goToLogin()
            }

            observeUnsubscribe()
        }
    }

    private fun observeUnsubscribe() {
        userViewModel.observeUserCorrectlyRemovedLiveData().observe(viewLifecycleOwner) {
            Toast.makeText(activity, it.detail, Toast.LENGTH_SHORT).show()
            goToLogin()
        }

        userViewModel.observeUserCorrectlyRemovedErrorLiveData().observe(viewLifecycleOwner) {
            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
            goToLogin()
        }
    }

    private fun showUnsubscribeConfirmationDialog(token: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Conferma")
            .setMessage("Sei sicuro di voler cancellare il tuo account?")
            .setPositiveButton("Si") { dialog, which ->
                userViewModel.authUnsubscribe(token)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun goToLogin() {
        DataSingleton.token = null
        DataSingleton.username = null

        ExtraUtil.clearTokenAndUsername(requireContext())

        val intent = Intent(activity, LoginActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }
}