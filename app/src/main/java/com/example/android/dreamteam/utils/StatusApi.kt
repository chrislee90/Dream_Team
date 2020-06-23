package com.example.android.dreamteam.utils

enum class UserApiStatus { LOADING, ERROR, DONE }

enum class UserFriendshipStatus { MYSELF, FRIEND, PENDING, REQUESTING, NONE }

enum class UserRoleStatus { NONE, KEEPER, DEF, MID, ATT }

enum class FragmentStatus { DEFAULT, SEARCHING, NO_RESULT, ERROR }

enum class FriendsListFragmentStatus { DEFAULT, NO_FRIEND, NO_REQUEST, ERROR }

enum class MatchFragmentStatus { DEFAULT, LOADING, NETWORK_ERROR, ERROR }

enum class MatchListFragmentStatus { DEFAULT, LOADING, NO_MATCHES, ERROR }

enum class MatchUserStatus { OWNER, PARTICIPANT, REQUESTING, INVITED, NONE}

enum class MatchListPlayersStatus { ACCEPTING, INVITING, FULL_REQUEST, FULL_INVITING, SHOWING }
