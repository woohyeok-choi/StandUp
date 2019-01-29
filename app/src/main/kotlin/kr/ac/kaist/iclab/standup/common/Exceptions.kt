package kr.ac.kaist.iclab.standup.common

import kr.ac.kaist.iclab.standup.R

abstract class StandUpException(val resId: Int? = null, message: String? = null) : Exception(message)

class EmptyResultException(message: String? = null) : StandUpException(R.string.msg_error_empty_data, message)