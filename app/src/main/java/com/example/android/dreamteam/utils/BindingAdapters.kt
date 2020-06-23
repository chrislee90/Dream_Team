package com.example.android.dreamteam.utils

import android.graphics.Color
import android.view.View
import android.view.animation.Transformation
import android.widget.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.android.dreamteam.R
import com.example.android.dreamteam.model.Match
import com.example.android.dreamteam.model.Notification
import com.example.android.dreamteam.model.User
import org.threeten.bp.format.DateTimeFormatter
import com.example.android.dreamteam.ui.account.AccountViewModel
import com.example.android.dreamteam.ui.match.MatchPlayersViewModel
import com.example.android.dreamteam.ui.match.MatchViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import java.util.*

@BindingAdapter("imageUrl")
fun ImageView.bindImage(imgUrl: String?) {
    imgUrl?.let {
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide.with(this.context)
            .load(imgUri)
            .circleCrop()
            .into(this)
    }
}

// Account Fragment

@BindingAdapter("picFriend")
fun ImageView.setPicFriend(item: User) {
    bindImage(item.picture)
}

@BindingAdapter("nameFriend")
fun TextView.setNameFriend(item: User) {
    text = item.name
}

@BindingAdapter("roleFriend")
fun TextView.setRoleFriend(item: User) {
    text = item.role
}

@BindingAdapter("dateString")
fun TextView.setDateString(date: Long){
    var instant = Instant.ofEpochSecond(date)
    var dateLocal = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    val day = dateLocal.dayOfWeek.name.substring(0,3)
    text = day+"\n\n" + dateLocal.format(DateTimeFormatter.ofPattern("[MM/dd]", Locale.ENGLISH))
}

@BindingAdapter("dateStringMatch")
fun TextView.setDateStringMatch(date: Long){
    var instant = Instant.ofEpochSecond(date)
    var dateLocal = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    val day = dateLocal.dayOfWeek.name
    text = day+" "+dateLocal.format(DateTimeFormatter.ofPattern("[MM/dd HH:mm]", Locale.ENGLISH))
}

@BindingAdapter("timeString")
fun TextView.timeFromDate(date: Long){
    var instant = Instant.ofEpochSecond(date)
    var dateLocal = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    text = dateLocal.format(DateTimeFormatter.ofPattern("[HH:mm]", Locale.ENGLISH))
}

@BindingAdapter("priceString")
fun TextView.setPriceString(price: Double){
    text = price.toString()
}

@BindingAdapter("leftParticipantsString")
fun TextView.setLeftParticipantsString(match: Match){
        text = (match.max_number?.minus(match.numberParticipants!!)).toString() + " left"
}




@BindingAdapter("userPrivacy")
fun TextView.setUserPrivacy(privacyRule: Boolean?){
    if (privacyRule == true) visibility = View.VISIBLE
    else if (privacyRule == false) visibility = View.GONE
}

@BindingAdapter("userApiStatus")
fun bindStatus(view: View,
               status: UserApiStatus?){
    when (status){
        UserApiStatus.LOADING -> {
            if(view is ImageView) {
                view.visibility = View.VISIBLE
                view.setImageResource(R.drawable.loading_animation)
            } else view.visibility = View.GONE
        }
        UserApiStatus.ERROR -> {
            if(view is ImageView) {
                view.visibility = View.VISIBLE
                view.setImageResource((R.drawable.ic_connection_error))
            } else view.visibility = View.GONE
        }
        UserApiStatus.DONE -> {
            if(view is ImageView) {
                view.visibility = View.GONE
            } else view.visibility = View.VISIBLE
        }
    }
}

// Friends Fragment

@BindingAdapter("bindTabStatus")
fun bindTabStatus(tabs: TabLayout, status: FragmentStatus?){
    when (status) {
        FragmentStatus.DEFAULT -> {
            tabs.visibility = View.VISIBLE
        }
        FragmentStatus.SEARCHING -> {
            tabs.visibility = View.GONE
        }
        FragmentStatus.NO_RESULT -> {
            tabs.visibility = View.GONE
        }
        FragmentStatus.ERROR -> {
            tabs.visibility = View.GONE
        }
    }
}

@BindingAdapter("bindViewPagerStatus")
fun bindViewPagerStatus(vp: ViewPager2, status: FragmentStatus?){
    when (status) {
        FragmentStatus.DEFAULT -> {
            vp.visibility = View.VISIBLE
        }
        FragmentStatus.SEARCHING -> {
            vp.visibility = View.GONE
        }
        FragmentStatus.NO_RESULT -> {
            vp.visibility = View.GONE
        }
        FragmentStatus.ERROR -> {
            vp.visibility = View.GONE
        }
    }
}

@BindingAdapter("bindFriendsViewStatus")
fun bindFriendsViewStatus(view: View, status: FragmentStatus?){
    when (status) {
        FragmentStatus.DEFAULT -> {
            view.visibility = View.GONE
        }
        FragmentStatus.SEARCHING -> {
            if(view is TextView)
                view.visibility = View.GONE
            else view.visibility = View.VISIBLE
        }
        FragmentStatus.NO_RESULT -> {
            if(view is TextView) {
                view.visibility = View.VISIBLE
                view.setText(R.string.search_no_result)
            } else view.visibility = View.GONE
        }
        FragmentStatus.ERROR -> {
            view.visibility = View.GONE
        }
    }
}

@BindingAdapter("bindFriendsListStatus")
fun bindFriendsListStatus(view: View, status: FriendsListFragmentStatus?){
    when (status) {
        FriendsListFragmentStatus.DEFAULT -> {
            if(view is TextView) view.visibility = View.GONE
            else view.visibility = View.VISIBLE
        }
        FriendsListFragmentStatus.NO_FRIEND -> {
            if(view is TextView) {
                view.visibility = View.VISIBLE
                view.setText(R.string.no_friends)
            } else view.visibility = View.GONE
        }
        FriendsListFragmentStatus.NO_REQUEST -> {
            if(view is TextView) {
                view.visibility = View.VISIBLE
                view.setText(R.string.no_requests)
            } else view.visibility = View.GONE
        }
        FriendsListFragmentStatus.ERROR -> {
            view.visibility = View.GONE
        }
    }
}

// Account Fragment -> Friendship Status

@BindingAdapter("status_pos", "view_pos")
fun bindFriendShipStatusPositive(button: Button, statusPos: UserFriendshipStatus?, viewPos: AccountViewModel){
    when (statusPos){
        UserFriendshipStatus.MYSELF -> {
            button.setText(R.string.account_edit)
            button.setBackgroundResource(R.color.colorAccent)
            button.setOnClickListener { viewPos.editProfile() }
        }
        UserFriendshipStatus.FRIEND -> {
            button.setText(R.string.account_friend)
            button.setBackgroundColor(Color.WHITE)
        }
        UserFriendshipStatus.PENDING -> {
            button.setText(R.string.account_sent_request)
            button.setBackgroundColor(Color.GRAY)
        }
        UserFriendshipStatus.REQUESTING -> {
            button.setText(R.string.account_accept)
            button.setBackgroundResource(R.color.lightcyan)
            button.setOnClickListener { viewPos.addFriend() }
        }
        UserFriendshipStatus.NONE -> {
            button.setText(R.string.account_add)
            button.setBackgroundResource(R.color.white)
            button.setOnClickListener{ viewPos.friendRequest() }
        }
    }
    button.visibility = View.VISIBLE
}

@BindingAdapter("status_neg", "view_neg")
fun bindFriendshipStatusNegative(button: Button, statusNeg: UserFriendshipStatus?, viewNeg: AccountViewModel){
    when (statusNeg){
        UserFriendshipStatus.MYSELF -> {
            button.visibility = View.GONE
        }
        UserFriendshipStatus.FRIEND -> {
            button.setText(R.string.account_remove)
            button.setBackgroundResource(R.color.bordeaux)
            button.setOnClickListener { viewNeg.deleteFriend() }
            button.visibility = View.VISIBLE
        }
        UserFriendshipStatus.PENDING -> {
            button.visibility = View.GONE
        }
        UserFriendshipStatus.REQUESTING -> {
            button.setText(R.string.account_reject)
            button.setBackgroundResource(R.color.bordeaux)
            button.setOnClickListener { viewNeg.denyFriendship() }
            button.visibility = View.VISIBLE

        }
        UserFriendshipStatus.NONE -> {
            button.visibility = View.GONE
        }
    }
}

// Edit Account Fragment

@BindingAdapter("editAccountRole")
fun editAccountRole(radioGroup: RadioGroup, role: UserRoleStatus?){
    when(role){
        UserRoleStatus.NONE -> {
            radioGroup.check(R.id.radioNull)
        }
        UserRoleStatus.KEEPER -> {
            radioGroup.check(R.id.radioKeeper)
        }
        UserRoleStatus.DEF -> {
            radioGroup.check(R.id.radioDefender)
        }
        UserRoleStatus.MID -> {
            radioGroup.check(R.id.radioMid)
        }
        UserRoleStatus.ATT -> {
            radioGroup.check(R.id.radioAtt)
        }
    }
}

// Home Fragment -> Match List


@BindingAdapter("bindMatchListStatus")
fun bindMatchListStatus(view: View, status: MatchListFragmentStatus?){
    when (status) {
        MatchListFragmentStatus.DEFAULT -> {
            if(view is TextView) view.visibility = View.GONE
            else view.visibility = View.VISIBLE
        }
        MatchListFragmentStatus.NO_MATCHES -> {
            if(view is TextView) {
                view.visibility = View.VISIBLE
                view.setText(R.string.match_no_matches)
            } else view.visibility = View.GONE
        }
        MatchListFragmentStatus.ERROR -> {
            view.visibility = View.GONE
        }
    }
}

@BindingAdapter("bindMatchListLayoutStatus")
fun bindMatchListLayoutStatus(swipeRefreshLayout: SwipeRefreshLayout, status: MatchListFragmentStatus?){
    when(status){
        MatchListFragmentStatus.DEFAULT -> {
            swipeRefreshLayout.visibility = View.VISIBLE
        }
        MatchListFragmentStatus.NO_MATCHES -> {
            swipeRefreshLayout.visibility = View.GONE
        }
        MatchListFragmentStatus.ERROR -> {
            swipeRefreshLayout.visibility = View.GONE
        }
    }
}

@BindingAdapter("bindHomeViewStatus")
fun bindHomeViewStatus(view: View, status: FragmentStatus?){
    when (status) {
        FragmentStatus.DEFAULT -> {
            view.visibility = View.GONE
        }
        FragmentStatus.SEARCHING -> {
            if(view is TextView)
                view.visibility = View.GONE
            else view.visibility = View.VISIBLE
        }
        FragmentStatus.NO_RESULT -> {
            if(view is TextView) {
                view.visibility = View.VISIBLE
                view.setText(R.string.match_no_result)
            } else view.visibility = View.GONE
        }
        FragmentStatus.ERROR -> {
            view.visibility = View.GONE
        }
    }
}

@BindingAdapter("bindFloatingButtonStatus")
fun bindFloatingButtonStatus(floatingActionButton: FloatingActionButton, status: FragmentStatus?){
    when (status) {
        FragmentStatus.DEFAULT -> {
            floatingActionButton.visibility = View.VISIBLE
        }
        else -> floatingActionButton.visibility = View.GONE
    }
}

// Notifications

@BindingAdapter("bindNotificationsViewStatus")
fun bindNotificationsViewStatus(view: View, status: FragmentStatus?){
    when (status) {
        FragmentStatus.DEFAULT -> {
            if(view is TextView) {
                view.visibility = View.GONE
            } else view.visibility= View.VISIBLE
        }
        FragmentStatus.NO_RESULT -> {
            if(view is TextView) {
                view.visibility = View.VISIBLE
                view.setText(R.string.notifications_no_notifications)
            } else view.visibility = View.GONE
        }
    }
}

@BindingAdapter("picNotification")
fun ImageView.setPicNotification(item: Notification) {
    bindImage(item.pictureUrl)
}

@BindingAdapter("messageNotification")
fun TextView.setMessageNotification(item: Notification) {
    text = item.message
}

@BindingAdapter("dateNotification")
fun TextView.setDateNotification(date: LocalDateTime){
    val day = date.dayOfWeek.name.substring(0,3)
    text = day+" "+date.format(DateTimeFormatter.ofPattern("[MM/dd]", Locale.ENGLISH))+" "+
            date.format(DateTimeFormatter.ofPattern("[HH:mm]", Locale.ENGLISH))
}

//Match View Model

@BindingAdapter("match_view_model", "match_user_status")
fun buttonToStatus(button: Button, matchViewModel: MatchViewModel, matchUserStatus: MatchUserStatus?){
    when(matchUserStatus){
        MatchUserStatus.OWNER -> {
            if(button.id == R.id.publish_match_button ||
                button.id == R.id.button_add_participant ||
                button.id == R.id.button_remove_participant){
                if(matchViewModel.today < matchViewModel.match.value!!.date) {
                    button.visibility = View.VISIBLE
                } else {
                    button.visibility = View.GONE
                }
            }
            if(button.id == R.id.show_participants_button) button.visibility = View.VISIBLE
            if(button.id == R.id.status_match_button){
                if(matchViewModel.match.value!!.isConfirmed!! &&
                        matchViewModel.today < matchViewModel.match.value!!.date)
                    button.visibility = View.VISIBLE
                else button.visibility = View.GONE
            }
            if(button.id == R.id.delete_match_button){
                if(matchViewModel.match.value!!.isConfirmed!!) button.visibility = View.GONE
                else button.visibility = View.VISIBLE
            }
            if(button.id == R.id.archive_match_button){
                if(matchViewModel.path.contains("/archivedMatches/")){
                    button.visibility = View.GONE
                } else {
                    if (matchViewModel.today >= matchViewModel.match.value!!.date)
                        button.visibility = View.VISIBLE
                    else button.visibility = View.GONE
                }
            }
            if(button.id == R.id.interaction1_match_button){
                if(matchViewModel.today < matchViewModel.match.value!!.date) {
                    button.visibility = View.VISIBLE
                    button.setText(R.string.match_show_requests)
                    button.setOnClickListener {
                        matchViewModel.showRequests()
                    }
                } else {
                    button.visibility = View.GONE
                }
            }
            if(button.id == R.id.interaction2_match_button){
                if(matchViewModel.today < matchViewModel.match.value!!.date) {
                    button.visibility = View.VISIBLE
                    button.setText(R.string.invite_button_text)
                    button.setOnClickListener {
                        matchViewModel.inviteFriends()
                    }
                } else {
                    button.visibility = View.GONE
                }
            }
        }
        MatchUserStatus.PARTICIPANT -> {
            if(button.id == R.id.delete_match_button ||
                button.id == R.id.publish_match_button ||
                button.id == R.id.archive_match_button ||
                button.id == R.id.button_add_participant ||
                button.id == R.id.button_remove_participant ||
                button.id == R.id.status_match_button) button.visibility = View.GONE
            if(button.id == R.id.show_participants_button) button.visibility = View.VISIBLE
            if(button.id == R.id.interaction2_match_button){
                if(matchViewModel.today < matchViewModel.match.value!!.date) {
                    button.visibility = View.VISIBLE
                    button.setText(R.string.match_leave)
                    button.setOnClickListener {
                        matchViewModel.leaveMatch()
                    }
                } else {
                    button.visibility = View.GONE
                }
            }
            if(button.id == R.id.interaction1_match_button){
                button.visibility = View.VISIBLE
                button.setText(R.string.match_is_participant)
            }
        }
        MatchUserStatus.INVITED -> {
            if(button.id == R.id.delete_match_button ||
                button.id == R.id.publish_match_button ||
                button.id == R.id.archive_match_button ||
                button.id == R.id.button_add_participant ||
                button.id == R.id.button_remove_participant ||
                button.id == R.id.status_match_button) button.visibility = View.GONE
            if(button.id == R.id.show_participants_button) button.visibility = View.VISIBLE
            if(button.id == R.id.interaction1_match_button){
                button.visibility = View.VISIBLE
                button.setText(R.string.match_accept)
                button.setOnClickListener {
                    matchViewModel.acceptInvitation()
                }
            }
            if(button.id == R.id.interaction2_match_button){
                button.visibility = View.VISIBLE
                button.setText(R.string.match_deny)
                button.setOnClickListener{
                    matchViewModel.denyInvitation()
                }
            }
        }
        MatchUserStatus.REQUESTING -> {
            if(button.id == R.id.delete_match_button ||
                button.id == R.id.publish_match_button ||
                button.id == R.id.archive_match_button ||
                button.id == R.id.button_add_participant ||
                button.id == R.id.button_remove_participant ||
                button.id == R.id.status_match_button) button.visibility = View.GONE
            if(button.id == R.id.show_participants_button) button.visibility = View.VISIBLE
            if(button.id == R.id.interaction1_match_button){
                button.visibility = View.VISIBLE
                button.setText(R.string.match_request_sent)
            }
            if(button.id == R.id.interaction2_match_button){
                button.visibility = View.GONE
            } 
        }
        MatchUserStatus.NONE -> {
            if(button.id == R.id.delete_match_button ||
                button.id == R.id.publish_match_button ||
                button.id == R.id.archive_match_button ||
                button.id == R.id.button_add_participant ||
                button.id == R.id.button_remove_participant ||
                    button.id == R.id.status_match_button) button.visibility = View.GONE
            if(button.id == R.id.show_participants_button) button.visibility = View.VISIBLE
            if(button.id == R.id.interaction1_match_button ){
                if(matchViewModel.match.value!!.numberParticipants != matchViewModel.match.value!!.max_number) {
                    button.visibility = View.VISIBLE
                    button.setText(R.string.match_request_participation)
                    button.setOnClickListener {
                        matchViewModel.requestParticipation()
                    }
                } else {
                    button.visibility = View.VISIBLE
                    button.setText(R.string.match_full)
                }
            }
            if(button.id == R.id.interaction2_match_button){
                button.visibility = View.GONE
            }
        }
    }
}

@BindingAdapter("bindPublishMatchStatus")
fun TextView.bindPublishMatchStatus(isPublic: Boolean){
    when(isPublic){
        true -> setText(R.string.private_match_button)
        false -> setText(R.string.public_match_button)
    }
}

@BindingAdapter("bindMatchStatus")
fun TextView.bindMatchStatus(isConfirmed: Boolean){
    when(isConfirmed){
        true -> setText(R.string.match_confirmed)
        false -> setText(R.string.match_cancelled)
    }
}

@BindingAdapter("match_players_view_model", "match_list_players_status", "user")
fun bindListUserButtons(button: Button, matchPlayersViewModel: MatchPlayersViewModel?,
                        matchListPlayersStatus: MatchListPlayersStatus?, user: User?){
    when(matchListPlayersStatus){
        MatchListPlayersStatus.ACCEPTING -> {
            when (button.id) {
                R.id.interaction2_list -> {
                    button.visibility = View.VISIBLE
                    button.setText(R.string.match_deny)
                    button.setBackgroundResource(R.color.bordeaux)
                }
                R.id.interaction1_list -> {
                    button.visibility = View.VISIBLE
                    button.setText(R.string.match_accept)
                    button.setBackgroundResource(R.color.lightcyan)
                }
            }
        }
        MatchListPlayersStatus.FULL_REQUEST -> {
            when (button.id) {
                R.id.interaction1_list -> {
                    button.visibility = View.VISIBLE
                    button.setBackgroundColor(Color.GRAY)
                    button.setText(R.string.match_full)
                }
                R.id.interaction2_list -> {
                    button.visibility = View.VISIBLE
                    button.setBackgroundResource(R.color.bordeaux)
                    button.setText(R.string.match_deny)
                }
            }
        }
        MatchListPlayersStatus.INVITING -> {
            when (button.id) {
                R.id.interaction1_list -> {
                    button.visibility = View.VISIBLE
                    button.setBackgroundResource(R.color.colorPrimary)
                    button.setText(R.string.match_add_friend)
                }
                R.id.interaction2_list -> {
                    button.visibility = View.GONE
                }
            }
        }
        MatchListPlayersStatus.FULL_INVITING -> {
            when (button.id) {
                R.id.interaction1_list -> {
                    button.visibility = View.VISIBLE
                    button.setBackgroundColor(Color.GRAY)
                    button.setText(R.string.match_full)
                }
                R.id.interaction2_list -> {
                    button.visibility = View.GONE
                }
            }
        }
        MatchListPlayersStatus.SHOWING -> {
            if (button.id == R.id.interaction1_list) {
                button.visibility = View.GONE
            }
            if (button.id == R.id.interaction2_list && matchPlayersViewModel!!.uid ==
                    matchPlayersViewModel.match.value!!.ownerId &&
                    user!!.uid != matchPlayersViewModel.match.value!!.ownerId &&
                !matchPlayersViewModel.path.contains("/archivedMatches/")) {
                button.visibility = View.VISIBLE
                button.setBackgroundResource(R.color.bordeaux)
                button.setText(R.string.match_expel_participant)
            } else if(button.id == R.id.interaction2_list) {
                button.visibility = View.GONE
            }
        }

        else -> button.visibility = View.GONE
    }
}

@BindingAdapter("bindMatchViewStatus")
fun bindMatchViewStatus(view: View, fragmentStatus: MatchFragmentStatus?){
    when(fragmentStatus) {
        MatchFragmentStatus.ERROR -> {
            when (view) {
                is TextView -> {
                    view.visibility = View.VISIBLE
                    view.setText(R.string.error_match)
                }
                is ImageView -> {
                    view.visibility = View.VISIBLE
                    view.setImageResource(R.drawable.ic_error_black_18dp)
                }
                else -> view.visibility = View.GONE
            }
        }
        MatchFragmentStatus.NETWORK_ERROR -> {
            when (view) {
                is TextView -> {
                    view.visibility = View.GONE
                }
                is ImageView -> {
                    view.visibility = View.VISIBLE
                    view.setImageResource(R.drawable.ic_connection_error)
                }
                else -> view.visibility = View.GONE
            }
        }
        MatchFragmentStatus.LOADING -> {
            when (view) {
                is TextView -> {
                    view.visibility = View.GONE
                }
                is ImageView -> {
                    view.visibility = View.VISIBLE
                    view.setImageResource(R.drawable.loading_animation)
                }
                else -> view.visibility = View.GONE
            }
        }
        else -> {
            if(view is TextView) view.visibility = View.GONE
            if(view is ImageView) view.visibility = View.GONE
            else view.visibility = View.VISIBLE
        }
    }
}

@BindingAdapter("matchType")
fun TextView.matchType(type: String?){
    when(type) {
        "Five-a-side" -> text = "5-a-side"
        "Eight-a-side" -> text = "8-a-side"
        else -> text = "Football"
    }
}