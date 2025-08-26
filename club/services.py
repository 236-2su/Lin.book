from langchain.schema import Document
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_community.vectorstores import FAISS

from .models import Club

EMBED_MODEL_NAME = "text-embedding-3-small"


def build_embedding_fn():
    return HuggingFaceEmbeddings(model_name=EMBED_MODEL_NAME)


def make_doc(club: Club) -> Document:
    text = f"{club.name} \n 분류: {club.major_category}/{club.minor_category}\n 키워드: {', '.join(club.tags or [])}\n 설명: {club.description or ''}\n 규모: {club.member_count or 'N/A'}\n"
    metadata = {
        "id": str(club.id),
        "major": club.major_category,
        "minor": club.minor_category,
        "member_count": club.member_count,
    }
    return Document(page_content=text, metadata=metadata)


def build_vectorstores(clubs_queryset, persist_path="faiss_index"):
    embed = build_embedding_fn()
    docs = [make_doc(c) for c in clubs_queryset]
    if not docs:
        raise ValueError("no docs")
    vs = FAISS.from_documents(docs, embed)
    vs.save_local(persist_path)
    return vs


def load_vectorstores(persist_path="faiss_index"):
    embed = build_embedding_fn()
    return FAISS.load_local(persist_path, embed, allow_dangerous_deserialization=True)


from typing import Dict, List, Optional


def similar_by_text(query: str, k: int = 10, filters: Optional[Dict] = None) -> List[Dict]:
    vs = load_vectorstores()
    docs = vs.similarity_search(query, k=k * 2)  # 여유 있게
    results = []
    for d in docs:
        m = d.metadata or {}
        if filters:
            # 예: major 필터
            if "major" in filters and m.get("major") != filters["major"]:
                continue
            # 예: 규모 상한/하한
            if "min_members" in filters and (m.get("member_count") or 0) < filters["min_members"]:
                continue
        results.append({"id": m.get("id"), "score_hint": None, "snippet": d.page_content[:180]})
        if len(results) >= k:
            break
    return results


def similar_by_club(club_id: str, k: int = 10) -> List[Dict]:
    club = Club.objects.get(id=club_id)
    query = make_doc(club).page_content
    return similar_by_text(query, k=k, filters={"major": club.major_category})
