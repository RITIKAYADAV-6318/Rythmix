package com.example.chasing.adapters

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.chasing.R
import com.example.chasing.models.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EventAdapter(private val eventList: List<Event>) :
    RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.eventTitle)
        val date: TextView = view.findViewById(R.id.eventDate)
        val desc: TextView = view.findViewById(R.id.eventDesc)
        val location: TextView = view.findViewById(R.id.eventLocation)
        val btnLink: Button = view.findViewById(R.id.btnEventLink)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        holder.title.text = event.title
        holder.date.text = event.date
        holder.desc.text = event.description

        // Normalize link
        val eventLink = event.location.trim()

        if (eventLink.isNotEmpty()) {
            holder.location.text = eventLink
            holder.location.visibility = View.VISIBLE
            holder.btnLink.visibility = View.VISIBLE
            
            holder.btnLink.setOnClickListener {
                try {
                    var url = eventLink
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, "Invalid link", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            holder.location.visibility = View.GONE
            holder.btnLink.visibility = View.GONE
        }

        // 🔥 Delete Option: Only visible to the creator of this specific event
        if (event.createdBy == currentUserId) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Event?")
                    .setMessage("Are you sure you want to remove '${event.title}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        FirebaseDatabase.getInstance().getReference("events").child(event.id).removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(holder.itemView.context, "Event Deleted", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount() = eventList.size
}