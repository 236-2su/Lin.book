from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("ledger", "0003_backfill_midnight_created_at"),
    ]

    operations = [
        migrations.AlterField(
            model_name="ledger",
            name="created_at",
            field=models.DateTimeField(auto_now_add=True),
        ),
        migrations.AlterField(
            model_name="receipt",
            name="created_at",
            field=models.DateTimeField(auto_now_add=True),
        ),
    ]
