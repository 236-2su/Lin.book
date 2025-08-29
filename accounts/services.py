import os
from datetime import date, datetime, timedelta

import requests
from rest_framework.exceptions import ValidationError

from club.models import ClubMember
from user.models import User


def _get_unique_transaction_no():
    """Generates a unique transaction number."""
    now = datetime.now()
    # YYYYMMDDHHMMSS (14) + microseconds (6) = 20 digits, which is very likely to be unique.
    return f"{now.strftime('%Y%m%d%H%M%S')}{now.microsecond:06d}"


def _get_api_key():
    api_key = os.getenv("FINAPI_SECRET")
    if not api_key:
        raise ValidationError({"error": "FINAPI_SECRET environment variable not set."})
    return api_key


def _make_request(url, data):
    """Makes a POST request to the external API."""
    try:
        response = requests.post(url, json=data)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        # It's good practice to log the error here
        raise ValidationError({"error": f"External API request failed: {str(e), response.json()}"})


def _build_header(api_name, user_key):
    """Builds the common header for API requests."""
    now = datetime.now()
    return {
        "apiName": api_name,
        "transmissionDate": now.strftime("%Y%m%d"),
        "transmissionTime": now.strftime("%H%M%S"),
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": api_name,
        "institutionTransactionUniqueNo": _get_unique_transaction_no(),
        "apiKey": _get_api_key(),
        "userKey": user_key,
    }


# API Service Functions


def create_account(user: User):
    """
    Calls the external API to create a demand deposit account.
    """
    api_name = "createDemandDepositAccount"
    url = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/createDemandDepositAccount"
    data = {
        "Header": _build_header(api_name, user.user_key),
        "accountTypeUniqueNo": "001-1-2d2921541edf42",
    }

    payload = _make_request(url, data)

    account_no = payload.get("REC", {}).get("accountNo")
    if not account_no:
        raise ValidationError({"error": "Failed to retrieve account number from external API.", "details": payload})
    return account_no


def get_account_info(user: User, account_no: str):
    """
    Calls the external API to inquire about a single account.
    """
    api_name = "inquireDemandDepositAccount"
    url = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccount"
    data = {
        "Header": _build_header(api_name, user.user_key),
        "accountNo": account_no,
    }

    payload = _make_request(url, data)
    return payload.get("REC", {})


def get_account_balance(user: User, account_no: str):
    """
    Calls the external API to inquire about account balance.
    """
    api_name = "inquireDemandDepositAccountBalance"
    url = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccountBalance"
    data = {
        "Header": _build_header(api_name, user.user_key),
        "accountNo": account_no,
    }

    payload = _make_request(url, data)
    return payload.get("REC", {})


def get_transaction_history(
    user: User,
    account_no: str,
    start_date: str | None = None,
    end_date: str | None = None,
    transaction_type: str = "A",
    order_by: str = "DESC",
):
    """
    Calls the external API to inquire transaction history.
    """
    if end_date is None:
        end_date = date.today().strftime("%Y%m%d")
    if start_date is None:
        start_date = (date.today() - timedelta(days=365)).strftime("%Y%m%d")
    api_name = "inquireTransactionHistoryList"
    url = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistoryList"
    data = {
        "Header": _build_header(api_name, user.user_key),
        "accountNo": account_no,
        "startDate": start_date,
        "endDate": end_date,
        "transactionType": transaction_type,
        "orderByType": order_by,
    }

    payload = _make_request(url, data)
    return payload.get("REC", {})


"""
거래 내역 조회 API 응답
{
    "totalCount": "1",
    "list": [
    {
        "transactionUniqueNo": "104429",
        "transactionDate": "20250830",
        "transactionTime": "014735",
        "transactionType": "1",
        "transactionTypeName": "입금(이체)",
        "transactionAccountNo": "0011461639385305",
        "transactionBalance": "100",
        "transactionAfterBalance": "100",
        "transactionSummary": "류하준 1월 회비",
        "transactionMemo": ""
    }
    ]
}
"""


def transfer(member: ClubMember, to_account_no: str, amount: int, withdrawal_message: str, deposit_message: str):
    user = member.user
    user_account = user.accounts_set.first()
    if not user_account:
        raise ValidationError("사용자에게 계좌가 없습니다.")

    api_name = "updateDemandDepositAccountTransfer"
    url = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateDemandDepositAccountTransfer"

    data = {
        "Header": _build_header(api_name, member.user.user_key),
        "depositAccountNo": to_account_no,
        "depositTransactionSummary": deposit_message,
        "transactionBalance": amount,
        "withdrawalAccountNo": user_account.code,
        "withdrawalTransactionSummary": withdrawal_message,
    }
    print(f"request for transfer, data:{data}")
    payload = _make_request(url, data)
    return payload


def deposit(user: User, amount: int, summary: str = "입금"):
    """
    Calls the external API to deposit into a user's account.
    """
    user_account = user.accounts_set.first()
    if not user_account:
        raise ValidationError(f"User {user.email} does not have an account.")

    api_name = "updateDemandDepositAccountDeposit"
    url = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateDemandDepositAccountDeposit"
    data = {
        "Header": _build_header(api_name, user.user_key),
        "accountNo": user_account.code,
        "transactionBalance": amount,
        "transactionSummary": summary,
    }

    return _make_request(url, data)
