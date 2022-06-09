// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/KbInstanceImpl.h"

#include <boost/format.hpp>

#include <algorithm>
#include <iomanip>
#include <iterator>
#include <map>
#include <ostream>
#include <stdexcept>
#include <string>
#include <vector>

namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::cbegin;
using ::std::cend;
using ::std::char_traits;
using ::std::dec;
using ::std::endl;
using ::std::hex;
using ::std::logic_error;
using ::std::make_pair;
using ::std::ostream;
using ::std::setfill;
using ::std::setw;
using ::std::vector;

bool pmnt::KbInstance::validate(ostream& s) const
{
	bool isKbValid = true;

	// Compile a list of all the resource offsets:
	::std::map<size_t, vector<ResourceId> > rsrcOffsetMap;
	size_t currentRsrcStart = 0;
	for (size_t i = 0; i < m_pi->m_uriTbl.size(); ++i)
	{
		const RsrcChar* p = m_pi->m_uriTbl.getRecordAt(i);
		if (*p == 0)
		{
			rsrcOffsetMap.insert(make_pair(currentRsrcStart, vector<ResourceId>()));
			currentRsrcStart = i + 1;
		}
	}

	// Map the fixed-length rsrc records onto the resource offsets:
	vector<ResourceId> wildRsrcList;
	ResourceId numRsrcs = rsrcCount();
	for (ResourceId rsrcId = 0; rsrcId < numRsrcs; ++rsrcId)
	{
		const KbRsrc& rsrc = m_pi->m_rsrcTbl.getRecordAt(rsrcId);
		if (!rsrc.isAnonymous())	// anonymous resources have no string representation
		{
			auto it = rsrcOffsetMap.find(rsrc.m_uriOffset);
			if (it == ::std::end(rsrcOffsetMap))
			{
				wildRsrcList.push_back(rsrcId);
			}
			else
			{
				it->second.push_back(rsrcId);
			}
		}

		//TODO: Check flags for validity
	}

	bool foundUnusedRsrcStr = false;
	for (auto it = cbegin(rsrcOffsetMap); it != cend(rsrcOffsetMap); ++it)
	{
		if (it->second.size() < 1)
		{
			isKbValid = false;
			if (!foundUnusedRsrcStr)
			{
				foundUnusedRsrcStr = true;
				s << endl << "Resource strings that are not used by any resources:" << endl;
			}
			size_t rsrcOffset = it->first;
			const RsrcChar* pRsrc = m_pi->m_uriTbl.getRecordAt(rsrcOffset);
			s << "   " << rsrcOffset << ":  " << pRsrc << endl;
		}
	}

	bool foundOverusedRsrcStr = false;
	for (auto it = cbegin(rsrcOffsetMap); it != cend(rsrcOffsetMap); ++it)
	{
		if (it->second.size() > 1)
		{
			isKbValid = false;
			if (!foundOverusedRsrcStr)
			{
				foundOverusedRsrcStr = true;
				s << endl << "Resource strings that are used by more than one resource:" << endl;
			}
			size_t rsrcOffset = it->first;
			const RsrcChar* pRsrc = m_pi->m_uriTbl.getRecordAt(rsrcOffset);
			s << "   " << rsrcOffset << ":  " << pRsrc << endl;

			bool firstTimeThrough = true;
			for (auto it2 = cbegin(it->second); it2 != cend(it->second); ++it2)
			{
				s << (firstTimeThrough ? "      " : ", ") << *it2;
				firstTimeThrough = false;
			}
			s << endl;
		}
	}

	if (wildRsrcList.size() > 0)
	{
		s << endl << "Resource records with a wild resource string offset:" << endl;
	}
	for (auto it = cbegin(wildRsrcList); it != cend(wildRsrcList); ++it)
	{
		isKbValid = false;
		ResourceId rsrcId = *it;
		const KbRsrc& rsrc = m_pi->m_rsrcTbl.getRecordAt(rsrcId);
		size_t totalUses = rsrc.m_subjectCount
			+ rsrc.m_predicateCount + rsrc.m_objectCount;

		s << endl << "resource[" << rsrcId << "]:  ???  (offset "
			<< rsrc.m_uriOffset << ")" << endl;
		s << "  flags: 0x" << hex << setfill('0') << setw(8) << rsrc.m_flags
			<< setfill(' ') << dec << ", # uses: " << totalUses;
		if (totalUses > 0)
		{
			s << " [subj: " << rsrc.m_subjectCount
				<< ", pred: " << rsrc.m_predicateCount
				<< ", obj: " << rsrc.m_objectCount << ']';
		}
		s << endl << endl;

		StmtIterator endIter = end();
		StmtIterator subIter = find(rsrcId, k_nullRsrcId, k_nullRsrcId);
		for (; subIter != endIter; ++subIter)
		{
			Statement stmt = subIter.statement();
			const char* pPrefix = stmt.isInferred()
				? "  I  <\?\?\?> <"
				: "     <\?\?\?> <";
			s << stmt.getStatementId() << pPrefix << formatRsrcUri(stmt.getPredicateId(), false)
				<< "> <" << formatRsrcUri(stmt.getObjectId(), false) << ">"
				<< endl;
		}

		StmtIterator prdIter = find(k_nullRsrcId, rsrcId, k_nullRsrcId);
		for (; prdIter != endIter; ++prdIter)
		{
			Statement stmt = prdIter.statement();
			const char* pPrefix = stmt.isInferred()
				? "  I  <"
				: "     <";
			s << stmt.getStatementId() << pPrefix << formatRsrcUri(stmt.getSubjectId(), false)
				<< "> <\?\?\?> <" << formatRsrcUri(stmt.getObjectId(), false) << ">"
				<< endl;
		}

		StmtIterator objIter = find(k_nullRsrcId, k_nullRsrcId, rsrcId);
		for (; objIter != endIter; ++objIter)
		{
			Statement stmt = objIter.statement();
			const char* pPrefix = stmt.isInferred()
				? "  I  <"
				: "     <";
			s << stmt.getStatementId() << pPrefix << formatRsrcUri(stmt.getSubjectId(), false)
				<< "> <" << formatRsrcUri(stmt.getPredicateId(), false) << "> <\?\?\?>"
				<< endl;
		}
	}
	return isKbValid;
}

bool pmnt::KbInstance::validateUriTblAgainstRsrcTbl(ostream& s) const
{
	using OffsetList = ::std::vector<size_t>;
	using RsrcIdList = ::std::vector<ResourceId>;

	bool isKbValid = true;

	// Map the fixed-length rsrc records onto the resource offsets:
	OffsetList encounteredOffsetsList;
	RsrcIdList noValidBitRsrcList;
	RsrcIdList badAnonRsrcList;
	RsrcIdList offTheEndWildRsrcList;
	RsrcIdList inTheMiddleWildRsrcList;
	for (ResourceId rsrcId = 0; rsrcId < rsrcCount(); ++rsrcId)
	{
		const KbRsrc& rsrc = m_pi->m_rsrcTbl.getRecordAt(rsrcId);
		if (!rsrc.isValid())
		{
			noValidBitRsrcList.push_back(rsrcId);
		}
		else if (rsrc.isAnonymous())
		{
			// Anonymous resources have no string representation:
			if (rsrc.m_uriOffset != KbRsrc::anonUriOffset())
			{
				badAnonRsrcList.push_back(rsrcId);
			}
		}
		else
		{
			if (rsrc.m_uriOffset >= m_pi->m_uriTbl.size())
			{
				offTheEndWildRsrcList.push_back(rsrcId);
			}
			else if (!isStartOfRsrcStr(rsrc.m_uriOffset))
			{
				inTheMiddleWildRsrcList.push_back(rsrcId);
			}
			else
			{
				encounteredOffsetsList.push_back(rsrc.m_uriOffset);
			}
		}
	}

	sort(::std::begin(encounteredOffsetsList), ::std::end(encounteredOffsetsList));
	size_t currentRsrcStart = 0;
	auto ofsIt = cbegin(encounteredOffsetsList);
	OffsetList unusedOffsetsList;
	OffsetList multiUsedOffsetsList;
	for (size_t i = 0; i < m_pi->m_uriTbl.size(); ++i)
	{
		const RsrcChar* p = m_pi->m_uriTbl.getRecordAt(i);
		if (*p == 0)
		{
			if (ofsIt == cend(encounteredOffsetsList) || *ofsIt > currentRsrcStart)
			{
				// currentRsrcStart is unused:
				unusedOffsetsList.push_back(currentRsrcStart);
			}
			else if (*ofsIt < currentRsrcStart)
			{
				// *ofsIt isn't a rsrc start -- this should never happen, as it
				// indicates a failure in the logic of the above iteration over m_rsrcTbl.
				throw logic_error(str(format(
					"encounteredOffsetsList is malformed at position %1%, bad value is %2%")
					% distance(cbegin(encounteredOffsetsList), ofsIt) % *ofsIt));
			}
			else
			{
				// Advance ofsIt to the next distinct value:
				size_t numOccurances = 0;
				while (ofsIt != cend(encounteredOffsetsList) && *ofsIt == currentRsrcStart)
				{
					++numOccurances;
					++ofsIt;
				}

				if (numOccurances == 0)
				{
					// This should never happen, as it indicates a logic error:
					throw logic_error(str(format("Logic error while processing "
						"encounteredOffsetsList (ofsIt is at end: %1%, *ofsIt: %2%, "
						"currentRsrcStart: %3%")
						% (ofsIt != cend(encounteredOffsetsList)) % *ofsIt % currentRsrcStart));
				}
				else if (numOccurances > 1)
				{
					// currentRsrcStart was used more than once:
					multiUsedOffsetsList.push_back(currentRsrcStart);
				}
			}
			currentRsrcStart = i + 1;
		}
	}
	if (currentRsrcStart != m_pi->m_uriTbl.size())
	{
		//TODO: report m_uriTbl ends with garbage
	}

	if (noValidBitRsrcList.size() > 0)
	{
		isKbValid = false;
		s << endl << "Resources whose valid bit is not set:" << endl;
	}
	for (auto it = cbegin(noValidBitRsrcList); it != cend(noValidBitRsrcList); ++it)
	{
		ResourceId rsrcId = *it;
		const RsrcChar* pRsrc = rsrcIdToUri(rsrcId);
		s << "   " << rsrcId << ":  " << pRsrc << endl;
	}

	if (badAnonRsrcList.size() > 0)
	{
		isKbValid = false;
		s << endl << "Anonymous resources whose offset is not null:" << endl;
	}
	for (auto it = cbegin(badAnonRsrcList); it != cend(badAnonRsrcList); ++it)
	{
		ResourceId rsrcId = *it;
		s << "   " << rsrcId << endl;
	}

	if (offTheEndWildRsrcList.size() > 0)
	{
		isKbValid = false;
		s << endl << "Resources whose offset is wild (off the end):" << endl;
	}
	for (auto it = cbegin(offTheEndWildRsrcList); it != cend(offTheEndWildRsrcList); ++it)
	{
		ResourceId rsrcId = *it;
		KbRsrc& rsrc = m_pi->m_rsrcTbl.getRecordAt(rsrcId);
		size_t rsrcOffset = rsrc.m_uriOffset;
		s << "   " << rsrcId << ":  " << rsrcOffset << endl;
	}

	if (inTheMiddleWildRsrcList.size() > 0)
	{
		isKbValid = false;
		s << endl << "Resources whose offset is wild (in the middle):" << endl;
	}
	for (auto it = cbegin(inTheMiddleWildRsrcList); it != cend(inTheMiddleWildRsrcList); ++it)
	{
		ResourceId rsrcId = *it;
		KbRsrc& rsrc = m_pi->m_rsrcTbl.getRecordAt(rsrcId);
		size_t rsrcOffset = rsrc.m_uriOffset;
		s << "   " << rsrcId << ":  " << rsrcOffset << endl;
	}

	if (unusedOffsetsList.size() > 0)
	{
		isKbValid = false;
		s << endl << "Resource strings that are not used by any resources:" << endl;
	}
	for (auto it = cbegin(unusedOffsetsList); it != cend(unusedOffsetsList); ++it)
	{
		size_t rsrcOffset = *it;
		const RsrcChar* pRsrc = m_pi->m_uriTbl.getRecordAt(rsrcOffset);
		s << "   " << rsrcOffset << ":  " << pRsrc << endl;
	}

	if (multiUsedOffsetsList.size() > 0)
	{
		isKbValid = false;
		s << endl << "Resource strings that are used by more than one resource:" << endl;
	}
	for (auto it = cbegin(multiUsedOffsetsList); it != cend(multiUsedOffsetsList); ++it)
	{
		size_t rsrcOffset = *it;
		const RsrcChar* pRsrc = m_pi->m_uriTbl.getRecordAt(rsrcOffset);
		s << "   " << rsrcOffset << ":  " << pRsrc << endl;
	}

	return isKbValid;
}

bool pmnt::KbInstance::isStartOfRsrcStr(size_t rsrcOffset) const
{
	bool result = false;

	if (rsrcOffset < m_pi->m_uriTbl.size())
	{
		if (rsrcOffset == 0)
		{
			result = true;
		}
		else
		{
			const RsrcChar* p = m_pi->m_uriTbl.getRecordAt(rsrcOffset - 1);
			if (*p == 0)
			{
				result = true;
			}
		}
	}
	return result;
}

// Assumes the m_uriTbl and m_rsrcTbl are correct and in sync
bool pmnt::KbInstance::validateStrToIdMapping(ostream& s) const
{
	using RsrcIdList = ::std::vector<ResourceId>;
	using RsrcIdPairList = ::std::vector< ::std::pair<ResourceId, ResourceId> >;
	using StrRsrcIdPairList = ::std::vector< ::std::pair< RsrcString, ResourceId> >;

	RsrcIdList rsrcsMissingFromBdb;
	RsrcIdPairList resourcesMisdirectedInBdb;
	RsrcIdList wildRsrcIdsInBdb;
	StrRsrcIdPairList bdbRsrcsThatDontMatchRsrcTbl;

	// Check each rsrc table entry against the BDB table:
	ResourceId numRsrcs = rsrcCount();
	for (ResourceId rsrcId = 0; rsrcId < numRsrcs; ++rsrcId)
	{
		const KbRsrc& rsrc = m_pi->m_rsrcTbl.getRecordAt(rsrcId);
		if (rsrc.isValid() && !rsrc.isAnonymous())
		{
			const RsrcChar* pRsrcStr = rsrcIdToUri(rsrcId);
			ResourceId rsrcId2 = m_pi->m_uriToRsrcId.find(pRsrcStr);
			if (rsrcId2 == k_nullRsrcId)
			{
				rsrcsMissingFromBdb.push_back(rsrcId);
			}
			else if (rsrcId2 != rsrcId)
			{
				resourcesMisdirectedInBdb.push_back(make_pair(rsrcId, rsrcId2));
			}
		}
	}

	// Check each BDB entry against the rsrc table:
	for (auto it = cbegin(m_pi->m_uriToRsrcId); it != cend(m_pi->m_uriToRsrcId); ++it)
	{
		const RsrcChar* pStr1 = it->first;
		ResourceId rsrcId = it->second;
		if (rsrcId >= numRsrcs) //TODO: Check valid flag
		{
			wildRsrcIdsInBdb.push_back(rsrcId);
		}
		else
		{
			//TODO: Check for anonymous
			const RsrcChar* pStr2 = rsrcIdToUri(rsrcId);
			size_t len1 = char_traits<RsrcChar>::length(pStr1);
			size_t len2 = char_traits<RsrcChar>::length(pStr2);
			if (len1 != len2 || char_traits<RsrcChar>::compare(pStr1, pStr2, len1) != 0)
			{
				bdbRsrcsThatDontMatchRsrcTbl.push_back(make_pair(pStr1, rsrcId));
			}
		}
	}

	// Report rsrcs missing from BDB:
	if (!rsrcsMissingFromBdb.empty())
	{
		s << endl << "Resource IDs that are missing from BDB:" << endl;
	}
	for (auto it = cbegin(rsrcsMissingFromBdb); it != cend(rsrcsMissingFromBdb); ++it)
	{
		s << "   " << *it << " <" << rsrcIdToUri(*it) << ">" << endl;
	}

	return rsrcsMissingFromBdb.empty()
		&& resourcesMisdirectedInBdb.empty()
		&& wildRsrcIdsInBdb.empty()
		&& bdbRsrcsThatDontMatchRsrcTbl.empty();
}
