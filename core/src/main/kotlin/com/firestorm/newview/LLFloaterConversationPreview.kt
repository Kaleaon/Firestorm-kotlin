package com.firestorm.newview

import java.util.UUID

const val LL_FCP_COMPLETE_NAME: String = "complete_name"
const val LL_FCP_ACCOUNT_NAME: String = "user_name"
private const val CONVERSATION_HISTORY_PAGE_SIZE: Int = 100

class LLFloaterConversationPreview(sessionId: LLSD) : LLFloater(sessionId) {

    private val mMutex: LLMutex = LLMutex()
    private var mPageSpinner: LLSpinCtrl? = null
    private var mChatHistory: FSChatHistory? = null
    private val mSessionID: UUID = sessionId.asUUID()
    private var mCurrentPage: Int = 0
    private val mPageSize: Int = CONVERSATION_HISTORY_PAGE_SIZE
    private var mMessages: MutableList<LLSD>? = null
    private val mAccountName: String = sessionId[LL_FCP_ACCOUNT_NAME].asString()
    private val mCompleteName: String = sessionId[LL_FCP_COMPLETE_NAME].asString()
    private var mChatHistoryFileName: String = ""
    private var mShowHistory: Boolean = false
    private var mHistoryThreadsBusy: Boolean = false
    private var mOpened: Boolean = false
    private var mIsGroup: Boolean = false

    override fun postBuild(): Boolean {
        mChatHistory = getChild<FSChatHistory>("chat_history")

        childSetAction("open_external_btn") { onBtnOpenExternal() }
        childSetAction("search_btn") { onClickSearch() }

        val conv = LLConversationLog.instance().getConversation(mSessionID)
        val name: String
        val file: String

        when {
            mAccountName.isNotEmpty() -> {
                name = mCompleteName
                file = mAccountName
            }
            mSessionID != UUID_NULL && conv != null -> {
                name = conv.getConversationName()
                file = conv.getHistoryFileName()
                mIsGroup = (LLIMModel.LLIMSession.GROUP_SESSION == conv.getConversationType())
            }
            else -> {
                name = LLTrans.getString("NearbyChatTitle")
                file = "chat"
            }
        }

        mChatHistoryFileName = file

        // Group chat logs may be stored without the suffix in conversation.log; append it so
        // the correct file is loaded even before the conversation is removed from the log.
        if (mIsGroup && !LLStringUtil.endsWith(mChatHistoryFileName, GROUP_CHAT_SUFFIX)) {
            mChatHistoryFileName += GROUP_CHAT_SUFFIX
        }

        val args = LLStringUtil.FormatMapT()
        args["[NAME]"] = name
        setTitle(getString("Title", args))

        return super.postBuild()
    }

    fun setPages(messages: MutableList<LLSD>?, fileName: String) {
        if (fileName == mChatHistoryFileName && messages != null) {
            synchronized(mMutex) {
                mMessages = messages
                mCurrentPage = if (messages.isNotEmpty()) (messages.size - 1) / mPageSize else 0

                val spinner = mPageSpinner
                if (spinner != null) {
                    spinner.setEnabled(true)
                    spinner.setMaxValue((mCurrentPage + 1).toFloat())
                    spinner.set((mCurrentPage + 1).toFloat())
                }

                val totalPageNum = "/ ${mCurrentPage + 1}"
                getChild<LLTextBox>("page_num_label").setValue(totalPageNum)
                mShowHistory = true
            }
        }
        val loadThread = LLLogChat.getInstance().getLoadHistoryThread(mSessionID)
        loadThread?.removeLoadEndSignal { msgs, fn -> setPages(msgs, fn) }
    }

    override fun draw() {
        if (mShowHistory) {
            showHistory()
            mShowHistory = false
        }
        super.draw()
    }

    override fun onOpen(key: LLSD) {
        if (mOpened) return
        mOpened = true

        if (!LLLogChat.getInstance().historyThreadsFinished(mSessionID)) {
            LLNotificationsUtil.add("ChatHistoryIsBusyAlert")
            mHistoryThreadsBusy = true
            closeFloater()
            return
        }

        val loadParams = LLSD()
        loadParams["load_all_history"] = true
        loadParams["cut_off_todays_date"] = false
        loadParams["is_group"] = mIsGroup

        val loadingMessages = mutableListOf<LLSD>()
        val loading = LLSD()
        loading[LL_IM_TEXT] = LLTrans.getString("loading_chat_logs")
        loadingMessages.add(loading)
        mMessages = loadingMessages

        val spinner = getChild<LLSpinCtrl>("history_page_spin")
        mPageSpinner = spinner
        spinner.setCommitCallback { onMoreHistoryBtnClick() }
        spinner.setMinValue(1.0)
        spinner.set(1.0)
        spinner.setEnabled(false)

        val messages = mutableListOf<LLSD>()

        val logChatInst = LLLogChat.getInstance()
        logChatInst.cleanupHistoryThreads()

        val loadThread = LLLoadHistoryThread(mChatHistoryFileName, messages, loadParams)
        loadThread.setLoadEndSignal { msgs, fn -> setPages(msgs as? MutableList<LLSD>, fn) }
        loadThread.start()
        logChatInst.addLoadHistoryThread(mSessionID, loadThread)

        val deleteThread = LLDeleteHistoryThread(messages, loadThread)
        logChatInst.addDeleteHistoryThread(mSessionID, deleteThread)

        mShowHistory = true
    }

    override fun onClose(appQuitting: Boolean) {
        mOpened = false
        if (!mHistoryThreadsBusy) {
            val deleteThread = LLLogChat.getInstance().getDeleteHistoryThread(mSessionID)
            deleteThread?.start()
        }
    }

    override fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (mask == MASK_CONTROL && key == 'F'.code) {
            LLFloaterSearchReplace.show(mChatHistory)
            return true
        }
        return super.handleKeyHere(key, mask)
    }

    override fun hasAccelerators(): Boolean = true

    private fun showHistory() {
        synchronized(mMutex) {
            val msgs = mMessages
            if (msgs == null || msgs.isEmpty() || mCurrentPage * mPageSize >= msgs.size) return

            mChatHistory?.clear()
            val iter = msgs.listIterator(mCurrentPage * mPageSize)
            var msgNum = 0
            while (iter.hasNext() && msgNum < mPageSize) {
                val msg = iter.next()

                var fromId: UUID = UUID_NULL
                val time = msg["time"].asString()
                val from = msg["from"].asString()
                val message = msg["message"].asString()

                if (msg["from_id"].isDefined()) {
                    fromId = msg["from_id"].asUUID()
                } else {
                    val legacyName = gCacheName.buildLegacyName(from)
                    fromId = LLAvatarNameCache.getInstance().findIdByName(legacyName)
                }

                val chat = LLChat()
                chat.mFromID = fromId
                chat.mSessionID = mSessionID
                chat.mFromName = from
                chat.mTimeStr = time
                chat.mChatStyle = CHAT_STYLE_HISTORY
                chat.mText = message

                chat.mSourceType = when {
                    fromId == UUID_NULL && SYSTEM_FROM == from -> CHAT_SOURCE_SYSTEM
                    fromId == UUID_NULL -> if (FSFloaterNearbyChat.isWordsName(from)) CHAT_SOURCE_UNKNOWN else CHAT_SOURCE_OBJECT
                    else -> chat.mSourceType
                }

                val chatArgs = LLSD()
                chatArgs["use_plain_text_chat_history"] = gSavedSettings.getBOOL("PlainTextChatHistory")
                chatArgs["show_time"] = gSavedSettings.getBOOL("FSShowTimestampsTranscripts")
                chatArgs["show_names_for_p2p_conv"] = gSavedSettings.getBOOL("IMShowNamesForP2PConv")
                chatArgs["conversation_log"] = true

                mChatHistory?.appendMessage(chat, chatArgs)
                msgNum++
            }
        }
    }

    private fun onMoreHistoryBtnClick() {
        val spinner = mPageSpinner ?: return
        mCurrentPage = spinner.getValueF32().toInt()
        if (mCurrentPage == 0) return
        mCurrentPage--
        mShowHistory = true
    }

    private fun onBtnOpenExternal() {
        val logFileName = LLLogChat.makeLogFileName(mChatHistoryFileName)
        if (gDirUtilp.fileExists(logFileName)) {
            gViewerWindow.getWindow().openFile(logFileName)
        } else {
            LLNotificationsUtil.add("ChatHistoryIsMissing")
        }
    }

    private fun onClickSearch() {
        LLFloaterSearchReplace.show(mChatHistory)
    }
}
