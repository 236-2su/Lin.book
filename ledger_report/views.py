import json

from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, OpenApiResponse, extend_schema
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from ledger.models import Ledger

from .models import LedgerReports
from .serializers import (
    LedgerReportsSerializer,
    MonthlyLedgerStatsResponseSerializer,
    YearlyLedgerStatsResponseSerializer,
)
from .services import (
    generate_report_advice_with_llm,
    generate_similar_clubs_yearly_report,
    monthly_ledger_stats,
    yearly_ledger_stats,
)


class MonthlyReportView(APIView):
    @extend_schema(
        summary="월간 보고서 목록 조회",
        description="해당 장부의 특정 년/월에 생성된 모든 월간 보고서 버전들을 조회합니다.",
        parameters=[
            OpenApiParameter("club_pk", int, OpenApiParameter.PATH, description="클럽 ID"),
            OpenApiParameter("ledger_pk", int, OpenApiParameter.PATH, description="장부 ID"),
            OpenApiParameter("year", int, OpenApiParameter.QUERY, description="조회할 년도"),
            OpenApiParameter("month", int, OpenApiParameter.QUERY, description="조회할 월"),
        ],
        responses={200: OpenApiResponse(LedgerReportsSerializer(many=True), description="OK")},
        tags=["LedgerReport"],
    )
    def get(self, request, club_pk: int, ledger_pk: int):
        ledger = get_object_or_404(Ledger, pk=ledger_pk, club_id=club_pk)

        try:
            year = int(request.query_params.get("year"))
            month = int(request.query_params.get("month"))
            if not (1 <= month <= 12):
                return Response({"detail": "month는 1~12 사이여야 합니다."}, status=status.HTTP_400_BAD_REQUEST)
        except (ValueError, TypeError):
            return Response({"detail": "year와 month는 필수 정수 값입니다."}, status=status.HTTP_400_BAD_REQUEST)

        base_title = f"{ledger.club.name}_{month}월_보고서"
        reports = LedgerReports.objects.filter(ledger=ledger, title__startswith=base_title).order_by("title")

        serializer = LedgerReportsSerializer(reports, many=True)
        return Response(serializer.data)

    @extend_schema(
        summary="월간 보고서 생성",
        description="해당 장부의 특정 년/월에 대한 월간 보고서를 새로 생성합니다. 최신 버전으로 자동 생성됩니다.",
        request=None,  # Body is empty, params in URL
        parameters=[
            OpenApiParameter("club_pk", int, OpenApiParameter.PATH, description="클럽 ID"),
            OpenApiParameter("ledger_pk", int, OpenApiParameter.PATH, description="장부 ID"),
            OpenApiParameter("year", int, OpenApiParameter.QUERY, description="생성할 년도"),
            OpenApiParameter("month", int, OpenApiParameter.QUERY, description="생성할 월"),
        ],
        responses={201: OpenApiResponse(MonthlyLedgerStatsResponseSerializer, description="Created")},
        tags=["LedgerReport"],
    )
    def post(self, request, club_pk: int, ledger_pk: int):
        get_object_or_404(Ledger, pk=ledger_pk, club_id=club_pk)

        try:
            year = int(request.query_params.get("year"))
            month = int(request.query_params.get("month"))
            if not (1 <= month <= 12):
                return Response({"detail": "month는 1~12 사이여야 합니다."}, status=status.HTTP_400_BAD_REQUEST)
        except (ValueError, TypeError):
            return Response({"detail": "year와 month는 필수 정수 값입니다."}, status=status.HTTP_400_BAD_REQUEST)

        report_data = monthly_ledger_stats(ledger_id=ledger_pk, year=year, month=month)
        serializer = MonthlyLedgerStatsResponseSerializer(report_data)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class YearlyReportView(APIView):
    @extend_schema(
        summary="연간 보고서 목록 조회",
        description="해당 장부의 특정 년도에 생성된 모든 연간 보고서 버전들을 조회합니다.",
        parameters=[
            OpenApiParameter("club_pk", int, OpenApiParameter.PATH, description="클럽 ID"),
            OpenApiParameter("ledger_pk", int, OpenApiParameter.PATH, description="장부 ID"),
            OpenApiParameter("year", int, OpenApiParameter.QUERY, description="조회할 년도"),
        ],
        responses={200: OpenApiResponse(LedgerReportsSerializer(many=True), description="OK")},
        tags=["LedgerReport"],
    )
    def get(self, request, club_pk: int, ledger_pk: int):
        ledger = get_object_or_404(Ledger, pk=ledger_pk, club_id=club_pk)

        try:
            year = int(request.query_params.get("year"))
        except (ValueError, TypeError):
            return Response({"detail": "year는 필수 정수 값입니다."}, status=status.HTTP_400_BAD_REQUEST)

        base_title = f"{ledger.club.name}_{year}년_보고서"
        reports = LedgerReports.objects.filter(ledger=ledger, title__startswith=base_title).order_by("title")

        serializer = LedgerReportsSerializer(reports, many=True)
        return Response(serializer.data)

    @extend_schema(
        summary="연간 보고서 생성",
        description="해당 장부의 특정 년도에 대한 연간 보고서를 새로 생성합니다. 최신 버전으로 자동 생성됩니다.",
        request=None,  # Body is empty, params in URL
        parameters=[
            OpenApiParameter("club_pk", int, OpenApiParameter.PATH, description="클럽 ID"),
            OpenApiParameter("ledger_pk", int, OpenApiParameter.PATH, description="장부 ID"),
            OpenApiParameter("year", int, OpenApiParameter.QUERY, description="생성할 년도"),
        ],
        responses={201: OpenApiResponse(YearlyLedgerStatsResponseSerializer, description="Created")},
        tags=["LedgerReport"],
    )
    def post(self, request, club_pk: int, ledger_pk: int):
        get_object_or_404(Ledger, pk=ledger_pk, club_id=club_pk)

        try:
            year = int(request.query_params.get("year"))
        except (ValueError, TypeError):
            return Response({"detail": "year는 필수 정수 값입니다."}, status=status.HTTP_400_BAD_REQUEST)

        report_data = yearly_ledger_stats(ledger_id=ledger_pk, year=year)
        serializer = YearlyLedgerStatsResponseSerializer(report_data)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class SimilarClubsYearlyReportView(APIView):
    @extend_schema(
        summary="유사 동아리 연간 보고서 생성",
        description="특정 동아리와 유사한 동아리 2개의 연간 보고서를 함께 생성하고 결과를 반환합니다.",
        request=None,
        parameters=[
            OpenApiParameter("club_id", int, OpenApiParameter.PATH, description="기준 동아리 ID"),
            OpenApiParameter("year", int, OpenApiParameter.PATH, description="조회할 년도"),
        ],
        responses={201: OpenApiResponse(description="Created")},
        tags=["LedgerReport"],
    )
    def post(self, request, club_id: int, year: int):
        reports = generate_similar_clubs_yearly_report(club_id=club_id, year=year)
        if "error" in reports:
            return Response(reports, status=status.HTTP_404_NOT_FOUND)
        return Response(reports, status=status.HTTP_201_CREATED)


class ReportAdviceView(APIView):
    @extend_schema(
        summary="연간 보고서 재무 조언 생성",
        description="""특정 장부의 해당 년도 최신 연간 보고서에 대해 LLM을 사용하여 재무 조언을 생성합니다.
        보고서가 없으면 새로 생성합니다.""",
        request=None,
        parameters=[
            OpenApiParameter("club_pk", int, OpenApiParameter.PATH, description="클럽 ID"),
            OpenApiParameter("ledger_pk", int, OpenApiParameter.PATH, description="장부 ID"),
            OpenApiParameter("year", int, OpenApiParameter.QUERY, description="조회 또는 생성할 년도"),
        ],
        responses={
            200: OpenApiResponse(description="OK - 조언이 포함된 JSON 응답"),
            400: OpenApiResponse(description="Bad Request - JSON 파싱 오류"),
            404: OpenApiResponse(description="Not Found - 해당 ID의 보고서 없음"),
        },
        tags=["LedgerReport"],
    )
    def post(self, request, club_pk: int, ledger_pk: int):
        ledger = get_object_or_404(Ledger, pk=ledger_pk, club_id=club_pk)
        try:
            year = int(request.query_params.get("year"))
        except (ValueError, TypeError):
            return Response({"detail": "year는 필수 정수 값입니다."}, status=status.HTTP_400_BAD_REQUEST)

        base_title = f"{ledger.club.name}_{year}년_보고서"
        report = LedgerReports.objects.filter(ledger=ledger, title__startswith=base_title).order_by("-title").first()

        if not report:
            yearly_ledger_stats(ledger_id=ledger_pk, year=year)
            report = (
                LedgerReports.objects.filter(ledger=ledger, title__startswith=base_title).order_by("-title").first()
            )

        report_data = report.content
        advice_json_str = generate_report_advice_with_llm(report_data)
        cleaned_advice_str = advice_json_str.strip().replace("```json", "").replace("```", "").strip()

        try:
            advice_data = json.loads(cleaned_advice_str)
            return Response(advice_data, status=status.HTTP_200_OK)
        except json.JSONDecodeError:
            return Response(
                {"error": "Failed to parse LLM response as JSON.", "raw_response": advice_json_str},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class ReportDetailView(APIView):
    @extend_schema(
        summary="보고서 상세 조회",
        description="특정 보고서의 상세 내용을 조회합니다.",
        parameters=[
            OpenApiParameter("report_pk", int, OpenApiParameter.PATH, description="레저 보고서 ID"),
        ],
        responses={200: OpenApiResponse(LedgerReportsSerializer, description="OK")},
        tags=["LedgerReport"],
    )
    def get(self, request, report_pk: int):
        report = get_object_or_404(LedgerReports, pk=report_pk)
        serializer = LedgerReportsSerializer(report)
        return Response(serializer.data)

    @extend_schema(
        summary="보고서 삭제",
        description="특정 보고서를 삭제합니다.",
        parameters=[
            OpenApiParameter("report_pk", int, OpenApiParameter.PATH, description="레저 보고서 ID"),
        ],
        responses={204: OpenApiResponse(description="No Content")},
        tags=["LedgerReport"],
    )
    def delete(self, request, report_pk: int):
        report = get_object_or_404(LedgerReports, pk=report_pk)
        report.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
