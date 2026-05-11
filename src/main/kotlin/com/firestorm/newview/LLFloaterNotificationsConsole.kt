package com.firestorm.newview

private const val NOTIFICATION_PANEL_HEADER_HEIGHT = 20
private const val HEADER_PADDING = 38

class LLNotificationChannelPanel(params: Params) : LLLayoutPanel(params) {

    private var channelPtr: LLNotificationChannelPtr? = null

    init {
        channelPtr = LLNotifications.instance().getChannel(params.name)
        buildFromFile("panel_notifications_channel.xml")
    }

    fun postBuild(): Boolean {
        val headerButton = getChild<LLButton>("header")
        headerButton.setLabel(channelPtr?.getName() ?: "")
        headerButton.setClickedCallback(::toggleClick, this)

        channelPtr?.connectChanged { payload: LLSD -> update(payload) }

        val scroll = getChild<LLScrollListCtrl>("notifications_list")
        scroll.setDoubleClickCallback(::onClickNotification, this)
        scroll.setRect(LLRect(getRect().left, getRect().top, getRect().right, 0))
        return true
    }

    fun destroy() {
        val dataList = getChild<LLScrollListCtrl>("notifications_list").getAllData()
        for (item in dataList) {
            item.setUserdata(null)
        }
    }

    private fun update(payload: LLSD): Boolean {
        val notification = LLNotifications.instance().find(payload["id"].asUUID())
        if (notification != null) {
            val row = LLSD()
            row["columns"][0]["value"] = notification.getName()
            row["columns"][0]["column"] = "name"

            row["columns"][1]["value"] = notification.getMessage()
            row["columns"][1]["column"] = "content"

            row["columns"][2]["value"] = notification.getDate()
            row["columns"][2]["column"] = "date"
            row["columns"][2]["type"] = "date"

            val sli = getChild<LLScrollListCtrl>("notifications_list").addElement(row)
            sli?.setUserdata(LLNotification(notification.asLLSD()))
        }
        return false
    }

    companion object {
        fun toggleClick(userData: Any?) {
            val self = userData as? LLNotificationChannelPanel ?: return
            val headerButton = self.getChild<LLButton>("header")
            val stack = self.getParent() as? LLLayoutStack
            stack?.collapsePanel(self, headerButton.getToggleState())
            self.getChild<LLScrollListCtrl>("notifications_list").setTabStop(!headerButton.getToggleState())
            self.getChild<LLScrollListCtrl>("notifications_list").setVisible(!headerButton.getToggleState())
        }

        fun onClickNotification(userData: Any?) {
            val self = userData as? LLNotificationChannelPanel ?: return
            val firstSelected = self.getChild<LLScrollListCtrl>("notifications_list").getFirstSelected()
            checkNotNull(firstSelected)
            val data = firstSelected?.getUserdata()
            if (data != null) {
                gFloaterView.getParentFloater(self)
                    ?.addDependentFloater(LLFloaterNotification(data as LLNotification), true)
            }
        }
    }
}

class LLFloaterNotificationConsole(key: LLSD) : LLFloater(key) {

    init {
        mCommitCallbackRegistrar.add("ClickAdd") { _: LLUICtrl, _: LLSD -> onClickAdd() }
    }

    fun postBuild(): Boolean {
        addChannel("Unexpired")
        addChannel("Ignore")
        addChannel("VisibilityRules")
        addChannel("Visible", open = true)
        addChannel("Persistent")
        addChannel("Alerts")
        addChannel("AlertModal")
        addChannel("Group Notifications")
        addChannel("Notifications")
        addChannel("NotificationTips")

        val notifications = getChild<LLComboBox>("notification_types")
        val names = LLNotifications.instance().getTemplateNames()
        for (name in names) {
            notifications.add(name)
        }
        notifications.sortByName()
        return true
    }

    fun addChannel(name: String, open: Boolean = false) {
        val stack = getChildRef<LLLayoutStack>("notification_channels")
        val p = LLNotificationChannelPanel.Params()
        p.minDim = NOTIFICATION_PANEL_HEADER_HEIGHT
        p.autoResize = true
        p.userResize = true
        p.name = name
        val panelp = LLNotificationChannelPanel(p)
        stack.addPanel(panelp, LLLayoutStack.ANIMATE)

        val headerButton = panelp.getChildRef<LLButton>("header")
        headerButton.setToggleState(!open)
        stack.collapsePanel(panelp, !open)

        updateResizeLimits()
    }

    fun removeChannel(name: String) {
        val panelp = getChild<LLPanel>(name)
        getChildRef<LLView>("notification_channels").removeChild(panelp)
        updateResizeLimits()
    }

    fun updateResizeLimits(stack: LLLayoutStack) {
        updateResizeLimits()
    }

    fun updateResizeLimits() {
        val floaterParams = LLFloater.getDefaultParams()
        val floaterHeaderSize = floaterParams.headerHeight

        val stack = getChildRef<LLLayoutStack>("notification_channels")
        setResizeLimits(
            getMinWidth(),
            floaterHeaderSize + HEADER_PADDING + (NOTIFICATION_PANEL_HEADER_HEIGHT + 3) * stack.getNumPanels()
        )
    }

    private fun onClickAdd() {
        val messageName = getChild<LLComboBox>("notification_types").getValue().asString()
        if (messageName.isNotEmpty()) {
            LLNotifications.instance().add(messageName, LLSD(), LLSD())
        }
    }
}

class LLFloaterNotification(private val note: LLNotification) : LLFloater(LLSD()) {

    init {
        buildFromFile("floater_notification.xml")
    }

    fun postBuild(): Boolean {
        setTitle(note.getName())
        getChild<LLUICtrl>("payload").setValue(note.getMessage())

        val responsesCombo = getChild<LLComboBox>("response")
        val responseList = responsesCombo.getListInterface()
        val form = note.getForm() ?: return true

        responsesCombo.setCommitCallback(::onCommitResponse, this)

        val formSd = form.asLLSD()
        for (formItem in formSd.arrayIterator()) {
            if (formItem["type"].asString() != "button") continue
            val text = formItem["text"].asString()
            responseList?.addSimpleElement(text)
        }
        return true
    }

    fun respond() {
        val responsesCombo = getChild<LLComboBox>("response")
        val responseList = responsesCombo.getListInterface()
        val trigger = responseList?.getSelectedValue()?.asString() ?: return

        val response = note.getResponseTemplate()
        response[trigger] = true
        note.respond(response)
    }

    companion object {
        fun onCommitResponse(ctrl: LLUICtrl?, data: Any?) {
            (data as? LLFloaterNotification)?.respond()
        }
    }
}
