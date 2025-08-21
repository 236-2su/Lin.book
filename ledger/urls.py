from django.urls import include, path
from rest_framework import routers
from rest_framework_nested import routers as nested_routers

from .views import LedgerTransactionsViewSet, LedgerViewSet, ReceiptViewSet

router = routers.SimpleRouter()
router.register(r"", LedgerViewSet, basename="ledger")

ledgers_router = nested_routers.NestedSimpleRouter(router, r"", lookup="ledger")
ledgers_router.register(r"transactions", LedgerTransactionsViewSet, basename="ledger-transactions")
ledgers_router.register(r"receipts", ReceiptViewSet, basename="ledger-receipts")

urlpatterns = [
    path("", include(router.urls)),
    path("", include(ledgers_router.urls)),
]
