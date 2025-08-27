from django.urls import path

from .views import MonthlyReportView, SimilarClubsYearlyReportView, YearlyReportView

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
    path(
        "report/similar-clubs/club/<int:club_id>/year/<int:year>/",
        SimilarClubsYearlyReportView.as_view(),
        name="similar-clubs-yearly-report",
    ),
]
