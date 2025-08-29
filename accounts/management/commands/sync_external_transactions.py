from datetime import datetime

from django.core.management.base import BaseCommand, CommandParser
from django.db import transaction
from django.utils import timezone

from accounts.models import Accounts
from accounts.services import get_transaction_history
from ledger.models import Ledger, LedgerTransactions


class Command(BaseCommand):
    help = "Synchronize transaction history from external API to local LedgerTransactions."

    def add_arguments(self, parser: CommandParser):
        parser.add_argument("--days", type=int, default=7, help="Number of past days to fetch transaction history for.")

    @transaction.atomic
    def handle(self, *args, **options):
        days_to_fetch = options["days"]
        self.stdout.write(f"Starting transaction synchronization for the last {days_to_fetch} days...")

        all_accounts = Accounts.objects.select_related("user", "club").all()

        if not all_accounts.exists():
            self.stdout.write(self.style.WARNING("No accounts found to synchronize."))
            return

        for account in all_accounts:
            if not account.club:
                self.stdout.write(self.style.WARNING(f"Account {account.code} is not linked to a club, skipping."))
                continue

            # Find the primary ledger for the club. Let's assume the first one is the primary.
            primary_ledger = Ledger.objects.filter(club=account.club).first()
            if not primary_ledger:
                self.stdout.write(
                    self.style.WARNING(
                        f"No ledger found for club {account.club.name}, skipping account {account.code}."
                    )
                )
                continue

            self.stdout.write(f"Fetching history for account: {account.code} (User: {account.user.email})...")

            try:
                history_data = get_transaction_history(user=account.user, account_no=account.code)
                transactions = history_data.get("list", [])

                if not transactions:
                    self.stdout.write(f"No new transactions found for account {account.code}.")
                    continue

                new_transactions_count = 0
                for tx in transactions:
                    external_id = tx.get("transactionUniqueNo")
                    if not external_id:
                        self.stderr.write(self.style.ERROR(f"Skipping transaction due to missing unique ID: {tx}"))
                        continue

                    if LedgerTransactions.objects.filter(external_id=external_id).exists():
                        continue  # Skip existing transaction

                    # Combine date and time and make it timezone-aware
                    tx_datetime_str = tx.get("transactionDate", "") + tx.get("transactionTime", "")
                    naive_datetime = datetime.strptime(tx_datetime_str, "%Y%m%d%H%M%S")
                    aware_datetime = timezone.make_aware(naive_datetime)

                    LedgerTransactions.objects.create(
                        ledger=primary_ledger,
                        external_id=external_id,
                        date_time=aware_datetime,
                        amount=int(tx.get("transactionBalance", 0)),
                        type=tx.get("transactionTypeName"),
                        payment_method="계좌이체",  # Assumption
                        description=tx.get("transactionSummary"),
                        vendor=tx.get("transactionSummary"),  # Or parse it from summary
                    )
                    new_transactions_count += 1

                if new_transactions_count > 0:
                    self.stdout.write(
                        self.style.SUCCESS(
                            f"Successfully synchronized {new_transactions_count} new transactions for account {account.code}."
                        )
                    )
                else:
                    self.stdout.write(f"No new transactions to synchronize for account {account.code}.")

            except Exception as e:
                self.stderr.write(
                    self.style.ERROR(f"Failed to synchronize transactions for account {account.code}: {e}")
                )

        self.stdout.write(self.style.SUCCESS("Transaction synchronization finished."))
