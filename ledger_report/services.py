# ledger_report/services.py
from calendar import monthrange
from datetime import date as dt_date
from typing import Optional

from django.db.models import Case, IntegerField, Sum, Value, When

from ledger.models import Ledger, LedgerTransactions  # 기존 앱의 모델 사용


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

    qs = LedgerTransactions.objects.filter(ledger=ledger, date__gte=start, date__lte=end)

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

    # 일자별(빈 날은 0으로 채움)
    daily_totals = dict(qs.values_list("date").annotate(total=Sum("amount")).values_list("date", "total"))
    daily_series = []
    for d in range(1, last_day + 1):
        day = dt_date(year, month, d)
        daily_series.append({"date": day.isoformat(), "total": int(daily_totals.get(day, 0) or 0)})

    return {
        "ledger_id": ledger.id,
        "club_id": ledger.club_id,
        "year": year,
        "month": month,
        "period": {"start": start.isoformat(), "end": end.isoformat()},
        "summary": {"income": int(income_sum), "expense": int(expense_sum), "net": int(net)},
        "by_type": by_type,
        "by_payment_method": by_payment_method,
        "daily_series": daily_series,
    }
