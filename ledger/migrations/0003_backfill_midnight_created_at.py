import datetime

from django.db import migrations
from django.utils import timezone


def to_midnight_aware(value):
    if value is None:
        return None
    if isinstance(value, datetime.datetime):
        d = value.date()
    elif isinstance(value, datetime.date):
        d = value
    else:
        return value
    dt = datetime.datetime.combine(d, datetime.time.min)
    if timezone.is_naive(dt):
        dt = timezone.make_aware(dt)
    return dt


def backfill_midnight(apps, schema_editor):
    Ledger = apps.get_model("ledger", "Ledger")
    Receipt = apps.get_model("ledger", "Receipt")

    # Ledger.created_at
    lqs = Ledger.objects.all().only("id", "created_at")
    l_to_update = []
    for obj in lqs.iterator():
        new = to_midnight_aware(obj.created_at)
        if new and new != obj.created_at:
            obj.created_at = new
            l_to_update.append(obj)
    if l_to_update:
        Ledger.objects.bulk_update(l_to_update, ["created_at"])

    # Receipt.created_at
    rqs = Receipt.objects.all().only("id", "created_at")
    r_to_update = []
    for obj in rqs.iterator():
        new = to_midnight_aware(obj.created_at)
        if new and new != obj.created_at:
            obj.created_at = new
            r_to_update.append(obj)
    if r_to_update:
        Receipt.objects.bulk_update(r_to_update, ["created_at"])


class Migration(migrations.Migration):

    dependencies = [
        ("ledger", "0002_alter_created_at_to_datetime_nullable"),
    ]

    operations = [
        migrations.RunPython(backfill_midnight, migrations.RunPython.noop),
    ]
