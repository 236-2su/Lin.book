# ledger_report/services.py
from calendar import monthrange
from datetime import date as dt_date
from typing import Optional

from django.db.models import Case, IntegerField, Sum, Value, When
from django.db.models.functions import TruncDate

from ledger.models import Event, Ledger, LedgerTransactions  # 기존 앱의 모델 사용

from .models import LedgerReports


def monthly_ledger_stats(
    ledger_id: int,
    year: Optional[int] = None,
    month: Optional[int] = None,
) -> dict:
    """
    특정 장부의 월간 통계를 계산해 dict로 반환.
    가정: 수입=양수, 지출=음수
    """
    ledger = Ledger.objects.select_related("club").get(pk=ledger_id)

    today = dt_date.today()
    year = year or today.year
    month = month or today.month

    start = dt_date(year, month, 1)
    last_day = monthrange(year, month)[1]
    end = dt_date(year, month, last_day)

    qs = LedgerTransactions.objects.filter(ledger=ledger, date_time__date__range=(start, end)).select_related("event")

    income_sum = qs.filter(amount__gt=0).aggregate(s=Sum("amount"))["s"] or 0
    expense_sum_raw = qs.filter(amount__lt=0).aggregate(s=Sum("amount"))["s"] or 0
    expense_sum = -expense_sum_raw  # 양수로 표기
    net = income_sum - expense_sum

    # type별
    type_rows = (
        qs.values("type")
        .annotate(
            income=Sum(Case(When(amount__gt=0, then="amount"), default=Value(0), output_field=IntegerField())),
            expense_raw=Sum(Case(When(amount__lt=0, then="amount"), default=Value(0), output_field=IntegerField())),
            net=Sum("amount"),
        )
        .order_by("-net")
    )
    by_type = []
    for r in type_rows:
        by_type.append(
            {
                "type": r["type"] or "기타",
                "income": int(r["income"] or 0),
                "expense": int(-(r["expense_raw"] or 0)),
                "net": int(r["net"] or 0),
            }
        )

    # 결제수단별
    pm_rows = (
        qs.values("payment_method")
        .annotate(
            income=Sum(Case(When(amount__gt=0, then="amount"), default=Value(0), output_field=IntegerField())),
            expense_raw=Sum(Case(When(amount__lt=0, then="amount"), default=Value(0), output_field=IntegerField())),
            net=Sum("amount"),
        )
        .order_by("-net")
    )
    by_payment_method = []
    for r in pm_rows:
        by_payment_method.append(
            {
                "payment_method": r["payment_method"] or "기타",
                "income": int(r["income"] or 0),
                "expense": int(-(r["expense_raw"] or 0)),
                "net": int(r["net"] or 0),
            }
        )

    # 이벤트별
    event_rows = (
        qs.values("event__name")
        .annotate(
            income=Sum(Case(When(amount__gt=0, then="amount"), default=Value(0), output_field=IntegerField())),
            expense_raw=Sum(Case(When(amount__lt=0, then="amount"), default=Value(0), output_field=IntegerField())),
            net=Sum("amount"),
        )
        .order_by("-net")
    )
    by_event = []
    for r in event_rows:
        by_event.append(
            {
                "event_name": r["event__name"] or "이벤트 미지정",
                "income": int(r["income"] or 0),
                "expense": int(-(r["expense_raw"] or 0)),
                "net": int(r["net"] or 0),
            }
        )

    # 일자별(빈 날은 0으로 채움)
    daily_totals = (
        qs.annotate(date=TruncDate("date_time"))
        .values("date")
        .annotate(total=Sum("amount"))
        .values_list("date", "total")
    )
    daily_totals_dict = dict(daily_totals)

    daily_series = []
    for d in range(1, last_day + 1):
        day = dt_date(year, month, d)
        daily_series.append({"date": day.isoformat(), "total": int(daily_totals_dict.get(day, 0) or 0)})

    stats_dict = {
        "ledger_id": ledger.id,
        "club_id": ledger.club_id,
        "year": year,
        "month": month,
        "period": {"start": start.isoformat(), "end": end.isoformat()},
        "summary": {"income": int(income_sum), "expense": int(expense_sum), "net": int(net)},
        "by_type": by_type,
        "by_payment_method": by_payment_method,
        "by_event": by_event,
        "daily_series": daily_series,
    }

    # 보고서 생성
    club_name = ledger.club.name
    base_title = f"{club_name}_{month}월_보고서"
    last_report = LedgerReports.objects.filter(ledger=ledger, title__startswith=base_title).order_by("-title").first()
    version = 1
    if last_report:
        try:
            last_version_str = last_report.title.split("_ver_")[-1]
            version = int(last_version_str) + 1
        except (ValueError, IndexError):
            version = LedgerReports.objects.filter(ledger=ledger, title__startswith=base_title).count() + 1
    final_title = f"{base_title}_ver_{version}"

    LedgerReports.objects.create(ledger=ledger, title=final_title, content=stats_dict)

    return stats_dict


def yearly_ledger_stats(
    ledger_id: int,
    year: Optional[int] = None,
):
    """
    특정 장부의 연간 통계를 계산해 dict로 반환.
    월별 상세 내역을 포함하며, 연간 전체 요약 정보를 추가로 제공합니다.
    NOTE: 월별 통계를 12번 호출하므로, 성능에 민감한 경우 최적화가 필요할 수 있습니다.
    """
    today = dt_date.today()
    year = year or today.year

    by_month_stats = {}
    total_income = 0
    total_expense = 0

    # 첫 달 통계를 먼저 가져와서 ledger_id 유효성 검사 및 club_id 확보
    first_month_stats = monthly_ledger_stats(ledger_id=ledger_id, year=year, month=1)
    by_month_stats[1] = first_month_stats
    total_income += first_month_stats["summary"]["income"]
    total_expense += first_month_stats["summary"]["expense"]
    club_id = first_month_stats["club_id"]

    for month in range(2, 13):
        monthly_data = monthly_ledger_stats(ledger_id=ledger_id, year=year, month=month)
        by_month_stats[month] = monthly_data
        total_income += monthly_data["summary"]["income"]
        total_expense += monthly_data["summary"]["expense"]

    stats_dict = {
        "ledger_id": ledger_id,
        "club_id": club_id,
        "year": year,
        "summary": {
            "income": total_income,
            "expense": total_expense,
            "net": total_income - total_expense,
        },
        "by_month": by_month_stats,
    }

    # 보고서 생성
    ledger = Ledger.objects.select_related("club").get(pk=ledger_id)
    club_name = ledger.club.name
    base_title = f"{club_name}_{year}년_보고서"
    last_report = LedgerReports.objects.filter(ledger=ledger, title__startswith=base_title).order_by("-title").first()
    version = 1
    if last_report:
        try:
            last_version_str = last_report.title.split("_ver_")[-1]
            version = int(last_version_str) + 1
        except (ValueError, IndexError):
            version = LedgerReports.objects.filter(ledger=ledger, title__startswith=base_title).count() + 1
    final_title = f"{base_title}_ver_{version}"

    LedgerReports.objects.create(ledger=ledger, title=final_title, content=stats_dict)

    return stats_dict
