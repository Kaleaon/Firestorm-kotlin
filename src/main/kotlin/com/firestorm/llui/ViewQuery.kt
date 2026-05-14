package com.firestorm.llui

typealias ViewList = MutableList<View>
typealias FilterResult = Pair<Boolean, Boolean>

abstract class QueryFilter {
    abstract fun invoke(view: View, children: ViewList): FilterResult
}

open class QuerySorter {
    open fun sort(parent: View, children: ViewList) {}
}

object LeavesFilter : QueryFilter() {
    override fun invoke(view: View, children: ViewList): FilterResult =
        FilterResult(children.isEmpty(), true)
}

object RootsFilter : QueryFilter() {
    override fun invoke(view: View, children: ViewList): FilterResult =
        FilterResult(true, false)
}

object VisibleFilter : QueryFilter() {
    override fun invoke(view: View, children: ViewList): FilterResult =
        FilterResult(view.visible, view.visible)
}

object EnabledFilter : QueryFilter() {
    override fun invoke(view: View, children: ViewList): FilterResult =
        FilterResult(view.enabled, view.enabled)
}

object TabStopFilter : QueryFilter() {
    override fun invoke(view: View, children: ViewList): FilterResult {
        val isTabStop = view is UICtrl && (view as UICtrl).hasTabStop()
        return FilterResult(isTabStop, view.canFocusChildren())
    }
}

object CtrlFilter : QueryFilter() {
    override fun invoke(view: View, children: ViewList): FilterResult =
        FilterResult(view is UICtrl, true)
}

class WidgetTypeFilter<T : View>(private val type: Class<T>) : QueryFilter() {
    override fun invoke(view: View, children: ViewList): FilterResult =
        FilterResult(type.isInstance(view), true)
}

inline fun <reified T : View> widgetTypeFilter(): WidgetTypeFilter<T> =
    WidgetTypeFilter(T::class.java)

open class ViewQuery {
    private val preFilters: MutableList<QueryFilter> = mutableListOf()
    private val postFilters: MutableList<QueryFilter> = mutableListOf()
    private var sorter: QuerySorter? = null

    fun addPreFilter(filter: QueryFilter) { preFilters.add(filter) }
    fun addPostFilter(filter: QueryFilter) { postFilters.add(filter) }
    fun getPreFilters(): List<QueryFilter> = preFilters
    fun getPostFilters(): List<QueryFilter> = postFilters

    fun setSorter(s: QuerySorter) { sorter = s }
    fun getSorter(): QuerySorter? = sorter

    operator fun invoke(view: View): ViewList = run(view)

    fun run(view: View): ViewList {
        val result: ViewList = mutableListOf()

        val pre = runFilters(view, view.children.toMutableList(), preFilters)
        if (!pre.first && !pre.second) return result

        val filteredChildren: ViewList = mutableListOf()
        var post = FilterResult(true, true)

        if (pre.second) {
            filterChildren(view, filteredChildren)
            if (pre.first) {
                post = runFilters(view, filteredChildren, postFilters)
            }
        }

        if (pre.first && post.first) result.add(view)
        if (pre.second && post.second) result.addAll(filteredChildren)

        return result
    }

    open fun filterChildren(parentView: View, filteredChildren: ViewList) {
        val childSnapshot = parentView.children.toMutableList()
        sorter?.sort(parentView, childSnapshot)
        for (child in childSnapshot) {
            filteredChildren.addAll(run(child))
        }
    }

    private fun runFilters(view: View, children: ViewList, filters: List<QueryFilter>): FilterResult {
        var includeThis = true
        var traverseChildren = true
        for (filter in filters) {
            val (inc, traverse) = filter.invoke(view, children)
            includeThis = includeThis && inc
            traverseChildren = traverseChildren && traverse
        }
        return FilterResult(includeThis, traverseChildren)
    }
}

fun View.canFocusChildren(): Boolean = false
