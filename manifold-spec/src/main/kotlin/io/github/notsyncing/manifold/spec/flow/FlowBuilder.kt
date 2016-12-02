package io.github.notsyncing.manifold.spec.flow

class FlowBuilder {
    private val start = FlowItem("Start")
    private val ends = mutableListOf<FlowExitItem>()
    private var currItem: FlowItem = start

    operator fun invoke(flow: FlowBuilder.() -> Unit) {
        this.flow()
    }

    fun on(cond: String): FlowCondItem {
        val item = FlowCondItem(cond)
        item.previous = currItem
        currItem.next = item
        currItem = item

        return item
    }

    fun goto(action: String): FlowActionItem {
        val item = FlowActionItem(action)
        item.previous = currItem
        currItem.next = item
        currItem = item

        return item
    }

    fun goto(item: FlowItem): FlowItem {
        item.previous = currItem
        currItem.next = item
        currItem = item

        return item
    }

    infix fun FlowCondItem.then(next: FlowItem): FlowCondItem {
        this.nextTrue = next
        next.previous = this

        return this
    }

    infix fun FlowCondItem.then(nextInner: FlowBuilder.() -> FlowItem): FlowCondItem {
        val oldCurr = currItem

        this@FlowBuilder.nextInner()

        currItem = oldCurr

        return this
    }

    infix fun FlowCondItem.or(next: FlowItem): FlowCondItem {
        this.nextFalse = next
        next.previous = this

        return this
    }

    infix fun FlowCondItem.or(nextInner: FlowBuilder.() -> FlowItem): FlowCondItem {
        val oldCurr = currItem

        this@FlowBuilder.nextInner()

        currItem = oldCurr

        return this
    }

    fun end(exitName: String): FlowExitItem {
        val item = FlowExitItem(exitName)
        ends.add(item)
        return item
    }

    fun build(): FlowInfo {
        return FlowInfo(start, ends)
    }
}