from django.urls import path

from .views import MonthlyReportView, YearlyReportView

urlpatterns = [
    path(
        "report/clubs/<int:club_pk>/ledgers/<int:ledger_pk>/reports/monthly/",
        MonthlyReportView.as_view(),
        name="monthly-report",
    ),
    path(
        "report/clubs/<int:club_pk>/ledgers/<int:ledger_pk>/reports/yearly/",
        YearlyReportView.as_view(),
        name="yearly-report",
    ),
]
