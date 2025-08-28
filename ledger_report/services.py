# ledger_report/services.py
import json
import os
from calendar import monthrange
from datetime import date as dt_date
from typing import Optional

import google.generativeai as genai
from django.db.models import Case, IntegerField, Sum, Value, When
from django.db.models.functions import TruncDate

from club.models import Club
from club.services import similar_by_club
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
    월별 상세 내역을 포함하며, 연간 전체 요약 및 항목별 집계를 추가로 제공합니다.
    """
    today = dt_date.today()
    year = year or today.year

    by_month_stats = {}
    total_income = 0
    total_expense = 0

    # 연간 항목별(by_type) 데이터 집계를 위한 딕셔너리
    total_by_type = {}

    # 첫 달 통계를 먼저 가져와서 ledger_id 유효성 검사 및 club_id 확보
    # NOTE: monthly_ledger_stats가 DB에 월별 보고서를 생성하므로, 이 구조를 유지합니다.
    first_month_stats = monthly_ledger_stats(ledger_id=ledger_id, year=year, month=1)
    by_month_stats[1] = first_month_stats
    total_income += first_month_stats["summary"]["income"]
    total_expense += first_month_stats["summary"]["expense"]
    club_id = first_month_stats["club_id"]

    # 첫 달의 by_type 데이터를 total_by_type에 집계
    for item in first_month_stats.get("by_type", []):
        type_name = item["type"]
        total_by_type[type_name] = {
            "income": item.get("income", 0),
            "expense": item.get("expense", 0),
        }

    for month in range(2, 13):
        monthly_data = monthly_ledger_stats(ledger_id=ledger_id, year=year, month=month)
        by_month_stats[month] = monthly_data
        total_income += monthly_data["summary"]["income"]
        total_expense += monthly_data["summary"]["expense"]

        # 월별 by_type 데이터를 total_by_type에 누적
        for item in monthly_data.get("by_type", []):
            type_name = item["type"]
            if type_name not in total_by_type:
                total_by_type[type_name] = {"income": 0, "expense": 0}
            total_by_type[type_name]["income"] += item.get("income", 0)
            total_by_type[type_name]["expense"] += item.get("expense", 0)

    stats_dict = {
        "ledger_id": ledger_id,
        "club_id": club_id,
        "year": year,
        "summary": {
            "income": total_income,
            "expense": total_expense,
            "net": total_income - total_expense,
        },
        "by_type": total_by_type,  # 연간 항목별 집계 데이터 추가
        "by_month": by_month_stats,
    }

    # 연간 보고서 DB 생성
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


def generate_similar_clubs_yearly_report(club_id: int, year: int):
    """
    주어진 동아리와 유사한 동아리 2개의 연간 보고서를 함께 생성하고 반환합니다.
    """
    # 1. 원본 동아리 및 유사 동아리 ID 가져오기
    try:
        original_club = Club.objects.get(pk=club_id)
    except Club.DoesNotExist:
        return {"error": f"Club with id {club_id} not found."}

    similar_clubs_raw = similar_by_club(club_id=str(club_id), k=2)
    similar_club_ids = [int(c["id"]) for c in similar_clubs_raw]

    all_club_ids = [club_id] + similar_club_ids
    all_reports = {"original_club_report": {}, "similar_club_reports": []}

    # 2. 각 동아리의 연간 보고서 생성
    for c_id in all_club_ids:
        try:
            # 각 동아리에는 하나의 대표 장부(Ledger)만 있다고 가정합니다.
            # 만약 여러 개일 경우, 어떤 장부를 선택할지에 대한 로직이 필요합니다.
            ledger = Ledger.objects.get(club_id=c_id)
            report = yearly_ledger_stats(ledger_id=ledger.id, year=year)

            if c_id == club_id:
                all_reports["original_club_report"] = report
            else:
                all_reports["similar_club_reports"].append(report)

        except Ledger.DoesNotExist:
            # 해당 동아리에 장부가 없는 경우, 보고서에 없음을 표시
            report_data = {"club_id": c_id, "year": year, "error": "Ledger not found for this club."}
            if c_id == club_id:
                all_reports["original_club_report"] = report_data
            else:
                all_reports["similar_club_reports"].append(report_data)
        except Exception as e:
            # 기타 예외 처리
            report_data = {"club_id": c_id, "year": year, "error": str(e)}
            if c_id == club_id:
                all_reports["original_club_report"] = report_data
            else:
                all_reports["similar_club_reports"].append(report_data)

    return all_reports


# === LLM Configuration === #
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
CHATBOT_LLM_MODEL = "gemini-2.5-pro"

genai.configure(api_key=GEMINI_API_KEY)


# === LLM Definition === #
class GeminiLLM:
    def __init__(self, model_name=CHATBOT_LLM_MODEL):
        self.model = genai.GenerativeModel(model_name)

    def __call__(self, prompt: str, **kwargs) -> str:
        try:
            response = self.model.generate_content(prompt, **kwargs)
            return response.text
        except Exception as e:
            return f"[Gemini API Error] {str(e)}"


def get_gemini_llm() -> GeminiLLM:
    return GeminiLLM()


def generate_report_advice_with_llm(report_data: dict) -> str:
    """
    연간 보고서 데이터를 LLM에 전달하여 재무 조언을 생성합니다.
    """
    # LLM에 전달할 프롬프트를 구성합니다.
    # JSON 데이터를 문자열로 변환하여 컨텍스트로 제공합니다.
    report_str = json.dumps(report_data, indent=2, ensure_ascii=False)

    system_prompt = "당신은 전문적인 동아리 재무 분석가입니다. 제공된 연간 재무 보고서 데이터를 바탕으로, 동아리 운영진이 다음 해 재무 계획을 세우는 데 도움이 될 만한 상세하고 구체적인 조언을 한국어로 작성해 주세요. 결과는 반드시 JSON 형태여야 합니다."

    prompt = f"""
{system_prompt}

아래는 우리 동아리의 {report_data.get('year')}년 연간 재무 보고서 데이터입니다.

[연간 재무 보고서 데이터]
{report_str}

[요청 사항]
위 데이터를 심층적으로 분석하여 다음 내용을 포함하는 재무 분석 보고서 및 조언을 작성해 주세요. 결과는 반드시 JSON 형태이어야 합니다.

1.  **총평:** 연간 총 수입, 총 지출, 순이익에 대한 요약 및 전반적인 재무 상태에 대한 평가.
2.  **월별 동향 분석:** 수입과 지출이 가장 많았던 달을 언급하고, 그 원인에 대해 추측해 보세요. 월별 순이익의 변동 추이를 설명해 주세요.
3.  **주요 지출 항목 분석:** 가장 큰 비중을 차지하는 지출 항목(type) 상위 2-3개를 식별하고, 해당 지출의 타당성과 절감 방안에 대해 조언해 주세요.
4.  **수입원 분석:** 주요 수입원의 특징을 분석하고, 수입 증대를 위한 아이디어를 제안해 주세요.
5.  **종합 제언:** 분석 내용을 바탕으로, 다음 해에 동아리가 재정적으로 더 발전하기 위한 구체적인 실행 계획이나 목표를 2~3가지 제안해 주세요.

보고서 결과는 API 출력으로 제공될 예정이므로 반드시 다음과 완전히 같은 JSON 형태여야 합니다.
{{
"overall" : string,
"by_month" : string,
"by_income" : string,
"advices" : [string, string...]
}}
"""

    # LLM을 호출하여 조언을 생성합니다.
    llm = get_gemini_llm()
    advice = llm(prompt)

    return advice
