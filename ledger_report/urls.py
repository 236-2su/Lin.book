from django.urls import path

from .views import MonthlyReportView, ReportAdviceView, ReportDetailView, SimilarClubsYearlyReportView, YearlyReportView

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
        "report/clubs/<int:club_pk>/ledgers/<int:ledger_pk>/advice/",
        ReportAdviceView.as_view(),
        name="report-advice",
    ),
    path(
        "report/similar-clubs/club/<int:club_id>/year/<int:year>/",
        SimilarClubsYearlyReportView.as_view(),
        name="similar-clubs-yearly-report",
    ),
    path("report/reports/<int:report_pk>/", ReportDetailView.as_view(), name="report-detail"),
]
