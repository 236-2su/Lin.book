from django.urls import include, path
from rest_framework import routers
from rest_framework_nested import routers as nested_routers

from ledger.views import LedgerTransactionsViewSet, LedgerViewSet, ReceiptViewSet

from .views import ClubMemberViewSet, ClubViewSet

router = routers.SimpleRouter()
router.register(r"", ClubViewSet, basename="club")

members_router = nested_routers.NestedSimpleRouter(router, r"", lookup="club")
members_router.register(r"members", ClubMemberViewSet, basename="club-members")

ledgers_router = nested_routers.NestedSimpleRouter(router, r"", lookup="club")
ledgers_router.register(r"ledger", LedgerViewSet, basename="club-ledgers")

transactions_router = nested_routers.NestedSimpleRouter(ledgers_router, r"ledger", lookup="ledger")
transactions_router.register(r"transactions", LedgerTransactionsViewSet, basename="ledger-transactions")

receipts_router = nested_routers.NestedSimpleRouter(ledgers_router, r"ledger", lookup="ledger")
receipts_router.register(r"receipts", ReceiptViewSet, basename="ledger-receipts")


urlpatterns = [
    path("", include(router.urls)),
    path("", include(members_router.urls)),
    path("", include(ledgers_router.urls)),
    path("", include(transactions_router.urls)),
    path("", include(receipts_router.urls)),
]
