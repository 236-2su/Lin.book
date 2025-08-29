from django.core.management.base import BaseCommand, CommandParser
from django.db import transaction

from accounts.models import Accounts
from accounts.services import deposit


class Command(BaseCommand):
    help = "Deposit a specified amount to all users who have a bank account."

    def add_arguments(self, parser: CommandParser):
        parser.add_argument("amount", type=int, help="The amount to deposit to each user.")
        parser.add_argument(
            "--summary", type=str, default="관리자 입금", help="The transaction summary for the deposit."
        )

    @transaction.atomic
    def handle(self, *args, **options):
        amount = options["amount"]
        summary = options["summary"]

        if amount <= 0:
            self.stderr.write(self.style.ERROR("The deposit amount must be positive."))
            return

        accounts_to_deposit = Accounts.objects.all()

        if not accounts_to_deposit.exists():
            self.stdout.write(self.style.WARNING("No accounts found in the system."))
            return

        self.stdout.write(f"Found {accounts_to_deposit.count()} accounts. Starting deposit of {amount} to each...")

        for account in accounts_to_deposit:
            user = account.user
            self.stdout.write(f"Depositing to account {account.code} for user: {user.email}...")
            try:
                # 1. Call the deposit service
                deposit(user=user, amount=amount, summary=summary)

                # 2. Update the local balance
                account.amount += amount
                account.save()

                self.stdout.write(
                    self.style.SUCCESS(
                        f"Successfully deposited {amount} to {user.email}. New balance: {account.amount}"
                    )
                )

            except Exception as e:
                self.stderr.write(self.style.ERROR(f"Failed to deposit to {user.email}: {e}"))

        self.stdout.write(self.style.SUCCESS("Finished deposit process."))
