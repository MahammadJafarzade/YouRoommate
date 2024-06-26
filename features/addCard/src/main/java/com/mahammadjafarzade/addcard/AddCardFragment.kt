package com.mahammadjafarzade.addcard

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mahammadjafarzade.addcard.databinding.FragmentAddCardBinding
import com.mahammadjafarzade.entities.model.RoomCard
import java.text.DateFormat
import java.util.Calendar

class AddCardFragment : Fragment() {
    private lateinit var binding: FragmentAddCardBinding
    private var imageURL: String = ""
    private var uri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddCardBinding.inflate(inflater, container, false)

        val activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    uri = data?.data
                    binding.uploadImage.setImageURI(uri)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No image selected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        binding.uploadImage.setOnClickListener {
            val photoPicker = Intent(Intent.ACTION_PICK)
            photoPicker.type = "image/*"
            activityResultLauncher.launch(photoPicker)
        }

        binding.saveButton.setOnClickListener {
            if (uri != null) {
                saveData()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please select an image first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return binding.root
    }

    private fun saveData() {
        uri?.let { uri ->
            val storageReference = FirebaseStorage.getInstance().reference.child("Room images")
                .child(uri.lastPathSegment!!)
            val builder = AlertDialog.Builder(requireContext())
            builder.setCancelable(false)
            builder.setView(R.layout.progress_bar)
            val dialog = builder.create()
            dialog.show()

            storageReference.putFile(uri).addOnSuccessListener { roomSnapshot ->
                val uriRoomCard = roomSnapshot.storage.downloadUrl
                uriRoomCard.addOnSuccessListener { urlImage ->
                    imageURL = urlImage.toString()
                    uploadData()
                    dialog.dismiss()
                    resetFields()
                }
            }.addOnFailureListener {
                dialog.dismiss()
            }
        }
    }

    private fun uploadData() {
        val title = binding.uploadTitle.text.toString()
        val description = binding.uploadDesc.text.toString()
        val price = binding.uploadPrice.text.toString().toDoubleOrNull()
        val city = binding.uploadCity.text.toString()
//        val profileTitle = binding.uploadProfileTitle.text.toString()
        val number = binding.uploadNumber.text.toString()

        // Assuming you have a way to get the UID and profile image
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val profileImg = "URL_TO_PROFILE_IMAGE" // Replace with actual URL to profile image

        val dataClass = RoomCard(
            uid = uid,
            image = imageURL,
            title = title,
            description = description,
            price = price,
            city = city,
            profileImg = profileImg,
            number = number// Convert to Int, use default value if conversion fails
        )


        val currentDate = DateFormat.getDateTimeInstance().format(Calendar.getInstance().time)
        FirebaseDatabase.getInstance().getReference("Room card").child(currentDate)
            .setValue(dataClass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Saved",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to save",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
    private fun showExitDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Are your sure exit from app?")
            .setPositiveButton("Yes") { _, _ ->
                requireActivity().finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.setOnShowListener {
            val messageView = dialog.findViewById<TextView>(android.R.id.message)
            messageView.setTextColor(Color.parseColor("#FFFFFFFF"))

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(Color.WHITE)

            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setTextColor(Color.WHITE)
        }
        dialog.show()

    }
    private fun resetFields() {
        binding.uploadImage.setImageDrawable(null)
        binding.uploadTitle.text.clear()
        binding.uploadDesc.text.clear()
        binding.uploadPrice.text.clear()
        binding.uploadCity.text.clear()
        binding.uploadNumber.text.clear()
    }
}


