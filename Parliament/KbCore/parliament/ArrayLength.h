// Taken with permission from "Effective Modern C++" by Scott Meyers
// (O'Reilly). Copyright 2015 Scott Meyers, 978-1-491-90399-5.

#if !defined(PARLIAMENT_ARRAYLENGTH_H_INCLUDED)
#define PARLIAMENT_ARRAYLENGTH_H_INCLUDED

#include <cstddef>

template<typename T, ::std::size_t N>
constexpr ::std::size_t arrayLen(T(&)[N]) noexcept
{
	return N;
}

#endif // !PARLIAMENT_ARRAYLENGTH_H_INCLUDED
