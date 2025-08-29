from django.urls import include, path
from rest_framework import routers
from rest_framework_nested import routers as nested_routers

from .views import LedgerTransactionCommentsViewSet, LedgerTransactionsViewSet, LedgerViewSet, ReceiptViewSet

router = routers.SimpleRouter()
router.lookup_value_regex = r"\d+"
router.register(r"ledgers", LedgerViewSet, basename="ledger")

ledgers_router = nested_routers.NestedSimpleRouter(router, r"ledgers", lookup="ledger")
ledgers_router.register(r"transactions", LedgerTransactionsViewSet, basename="ledger-transactions")
ledgers_router.register(r"receipts", ReceiptViewSet, basename="ledger-receipts")

# Comments are nested under transactions
transactions_router = nested_routers.NestedSimpleRouter(ledgers_router, r"transactions", lookup="transaction")
transactions_router.register(r"comments", LedgerTransactionCommentsViewSet, basename="transaction-comments")


urlpatterns = [
    path("", include(router.urls)),
    path("", include(ledgers_router.urls)),
    path("", include(transactions_router.urls)),
]
