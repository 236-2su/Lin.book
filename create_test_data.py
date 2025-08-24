#!/usr/bin/env python
import os
import django

# Django 설정 초기화
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

from user.models import User
from club.models import Club, ClubMember
from boards.models import Board

def create_test_data():
    print("테스트 데이터 생성 시작...")
    
    # 1. User 생성
    user, created = User.objects.get_or_create(
        email="test@example.com",
        defaults={
            'name': '테스트 사용자',
            'student_number': '20240001',
            'admission_year': 2024
        }
    )
    print(f"User 생성: {user.email} (created: {created})")
    
    # 2. Club 생성
    club, created = Club.objects.get_or_create(
        id=1,
        defaults={
            'name': '짱구네 코딩',
            'department': '컴퓨터공학과',
            'major_category': 'academic',
            'minor_category': '프로그래밍',
            'description': '코딩을 좋아하는 사람들의 모임',
            'hashtags': '#코딩 #프로그래밍 #개발'
        }
    )
    print(f"Club 생성: {club.name} (ID: {club.id}, created: {created})")
    
    # 3. ClubMember 생성
    member, created = ClubMember.objects.get_or_create(
        user=user,
        club=club,
        defaults={
            'status': 'active',
            'role': 'leader',
            'amount_fee': 0,
            'paid_fee': 0
        }
    )
    print(f"ClubMember 생성: {member.user.name} (created: {created})")
    
    # 4. Board 생성 (공지사항)
    board1, created = Board.objects.get_or_create(
        id=1,
        defaults={
            'type': 'announcement',
            'title': '2025년 3분기 정기 총회',
            'content': '안녕하세요! 짱구네 코딩 운영진입니다 😀 2025년 3분기 정기 총회를 아래와 같이 개최하오니, 모든 동아리원들의 많은 참여 바랍니다.',
            'views': 13,
            'author': member,
            'club': club
        }
    )
    print(f"Board 1 생성: {board1.title} (created: {created})")
    
    # 5. Board 생성 (자유게시판)
    board2, created = Board.objects.get_or_create(
        id=2,
        defaults={
            'type': 'forum',
            'title': '프로젝트 아이디어 공유',
            'content': '다음 프로젝트 아이디어를 공유해보세요!',
            'views': 5,
            'author': member,
            'club': club
        }
    )
    print(f"Board 2 생성: {board2.title} (created: {created})")
    
    print("\n테스트 데이터 생성 완료!")
    print(f"Club ID: {club.id}")
    print(f"Board count: {Board.objects.count()}")
    print(f"Announcement count: {Board.objects.filter(type='announcement').count()}")

if __name__ == '__main__':
    create_test_data()
