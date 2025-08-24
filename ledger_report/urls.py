from django.urls import path

from .views import MonthlyLedgerReportView

urlpatterns = [
    path(
        "report/clubs/<int:club_pk>/ledgers/<int:ledger_pk>/reports/monthly/",
        MonthlyLedgerReportView.as_view(),
        name="ledger-monthly-report",
    ),
]
