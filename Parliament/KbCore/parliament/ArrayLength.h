// Taken with permission from "Effective Modern C++" by Scott Meyers
// (O'Reilly). Copyright 2015 Scott Meyers, 978-1-491-90399-5.

#if !defined(PARLIAMENT_ARRAYLENGTH_H_INCLUDED)
#define PARLIAMENT_ARRAYLENGTH_H_INCLUDED

#include <cstddef>

namespace bbn::parliament
{

template<typename T, ::std::size_t N>
constexpr ::std::size_t arrayLen(T(&)[N]) noexcept
{
	return N;
}

}	// namespace end

#endif // !PARLIAMENT_ARRAYLENGTH_H_INCLUDED
