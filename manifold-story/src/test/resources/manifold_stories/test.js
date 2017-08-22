"use strict";

var customer = new Role("Orders", "View");
var salesman = new Role("Orders", "View");
var auditor = new Role("OrderAudit", "Edit");
var accounter = new Role("OrderCheck", "Edit");
var worker = new Role("OrderDone", "Edit");
var servicer = new Role("OrderArchive", "Edit");

var OrderRepository = repo("io.github.notsyncing.manifold.story.OrderRepository");

auditor.on("audit", function (context, params) {
    var orders = new OrderRepository(context);
    context.useTransaction();

    return orders.get({ id: params.orderId })
        .then(function (order) {
            order.status = OrderStatus.Audited;
            return orders.save(order);
        })
        .then(function () {
            context.emit("manifold.order.audited", { orderId: params.orderId });
        });
});

accounter.on("check", function (p) {
    var order = orderRepository.get({ id: p.orderId });
    order.status = OrderStatus.Checked;
});

worker.on("done", function (p) {
    var order = orderRepository.get({ id: p.orderId });
    order.status = OrderStatus.Installed;
});

servicer.on("archive", function (p) {
    var order = orderRepository.get({ id: p.orderId });
    order.status = OrderStatus.Archived;
});